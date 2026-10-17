package com.josie.tripstar.service.impl;

import com.josie.tripstar.service.TripService;
import com.josie.tripstar.service.TripPlannerService;
import com.josie.tripstar.service.PreferenceExtractor;

import com.josie.tripstar.mapper.TripMapper;
import com.josie.tripstar.entity.*;
import com.josie.tripstar.entity.enums.TaskStatus;
import com.josie.tripstar.entity.exception.BusinessException;
import com.josie.tripstar.entity.vo.*;
import com.josie.tripstar.utils.JsonUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import lombok.extern.slf4j.Slf4j;
import java.util.*;
import java.util.concurrent.RejectedExecutionException;
import java.util.stream.Collectors;

/** 规划业务入口；模型网络请求不持有数据库事务或行锁。 */
@Slf4j
@Service
public class TripServiceImpl implements TripService {
    private final TripMapper mapper;
    private final TripPlannerService planner;
    private final PreferenceExtractor extractor;
    private final JsonUtils json;
    private final TransactionTemplate transactions;
    private final ThreadPoolTaskExecutor executor;

    public TripServiceImpl(TripMapper mapper, TripPlannerService planner, PreferenceExtractor extractor, JsonUtils json,
                       TransactionTemplate transactions, @Qualifier("tripExecutor") ThreadPoolTaskExecutor executor) {
        this.mapper = mapper; this.planner = planner; this.extractor = extractor; this.json = json;
        this.transactions = transactions; this.executor = executor;
    }

    @Override
    public TripTaskVO submit(Long userId, TripRequest request) {
        planner.normalize(request);
        // 固定请求快照，避免后台任务使用被调用方继续修改的对象。
        String requestJson = json.write(request);
        boolean[] created = {false};
        TripTask task = transactions.execute(status -> {
            TripPlan plan;
            if (request.getPlanId() == null || request.getPlanId().trim().isEmpty()) {
                plan = new TripPlan(); plan.setPublicId(UUID.randomUUID().toString());
                plan.setUserId(userId); plan.setTitle(request.getCity() + " " + request.getTravelDays() + "日游");
                mapper.insertPlan(plan);
            } else {
                plan = requirePlan(userId, request.getPlanId());
                // 同一计划的幂等检查与任务插入串行化，不依赖进程内锁。
                mapper.lockPlan(plan.getId());
            }
            TripTask existing = mapper.taskByKey(plan.getId(), request.getIdempotencyKey());
            if (existing != null) {
                TripRequest previous = json.read(existing.getRequestJson(), TripRequest.class);
                // plan_id 只是定位参数，不参与请求业务内容比较。
                previous.setPlanId(null);
                TripRequest incoming = json.read(requestJson, TripRequest.class); incoming.setPlanId(null);
                if (!previous.equals(incoming)) throw BusinessException.conflict("同一幂等键不能用于不同的规划内容");
                return existing;
            }
            TripTask fresh = new TripTask();
            fresh.setPublicId(UUID.randomUUID().toString()); fresh.setPlanId(plan.getId());
            fresh.setIdempotencyKey(request.getIdempotencyKey()); fresh.setRequestJson(requestJson);
            mapper.insertTask(fresh); created[0] = true;
            return mapper.taskById(fresh.getId());
        });
        // 提交事务成功后再派发，工作线程不会读到未提交记录。
        if (created[0]) {
            try { executor.execute(() -> run(task.getId(), userId)); }
            catch (RejectedExecutionException e) { mapper.failTask(task.getId(), "QUEUE_FULL", "规划队列已满，请使用新幂等键稍后重试"); }
        }
        return status(userId, task.getPublicId());
    }

    private void run(Long taskId, Long userId) {
        try {
            if (mapper.claimTask(taskId) != 1) return;
            TripTask task = mapper.taskById(taskId);
            TripRequest request = json.read(task.getRequestJson(), TripRequest.class);
            TripPlanVO result = planner.generate(userId, request);
            if (mapper.progress(taskId) != 1) return;
            boolean saved = Boolean.TRUE.equals(transactions.execute(status -> {
                // 始终先锁计划、再锁任务，保持加锁顺序；同计划版本号生成不会冲突。
                mapper.lockPlan(task.getPlanId());
                TripTask locked = mapper.lockTask(taskId);
                if (!TaskStatus.PROCESSING.getValue().equals(locked.getStatus())) return false;
                TripPlanVersion version = new TripPlanVersion();
                version.setPlanId(task.getPlanId()); version.setTaskId(taskId);
                version.setVersionNo(mapper.nextVersion(task.getPlanId())); version.setMainCity(result.getCity());
                version.setStartDate(result.getStartDate()); version.setEndDate(result.getEndDate());
                version.setTravelDays(request.getTravelDays()); version.setLanguage(request.getLanguage());
                version.setTotalBudget(result.getBudget().getTotal()); version.setCurrency("CNY");
                version.setPlanJson(json.write(result)); version.setSchemaVersion(1);
                mapper.insertVersion(version);
                if (mapper.completeTask(taskId) != 1) throw BusinessException.conflict("任务已终止");
                return true;
            }));
            if (saved) extractor.extract(userId, request);
        } catch (Exception e) {
            String code = e instanceof BusinessException ? ((BusinessException) e).getCode() : "PLANNING_FAILED";
            String message = e instanceof BusinessException ? e.getMessage() : "旅行规划失败，请稍后重试";
            log.warn("规划任务 {} 失败，异常类型：{}", taskId, e.getClass().getSimpleName());
            mapper.failTask(taskId, code, message);
        }
    }

    @Override
    public TripTaskVO status(Long userId, String taskId) {
        TripTask task = mapper.findTask(taskId, userId);
        if (task == null) throw BusinessException.notFound();
        TripTaskVO result = new TripTaskVO();
        TripPlan plan = mapper.findPlanById(task.getPlanId());
        result.setTaskId(task.getPublicId()); result.setPlanId(plan.getPublicId());
        result.setStatus(task.getStatus()); result.setStage(task.getStage()); result.setProgress(task.getProgress());
        result.setMessage(task.getMessage()); result.setError(task.getErrorMessage()); result.setErrorCode(task.getErrorCode());
        if (TaskStatus.COMPLETED.getValue().equals(task.getStatus())) result.setResult(view(plan, mapper.versionByTask(task.getId())));
        return result;
    }

    @Override
    public TripPlan requirePlan(Long userId, String publicId) {
        TripPlan plan = mapper.findPlan(publicId, userId);
        if (plan == null) throw BusinessException.notFound();
        return plan;
    }

    @Override
    public TripPlanVersion requireVersion(Long userId, String publicId, int versionNo) {
        TripPlan plan = requirePlan(userId, publicId);
        TripPlanVersion version = mapper.version(plan.getId(), versionNo);
        if (version == null) throw BusinessException.notFound();
        return version;
    }

    @Override
    public PlanVersionVO version(Long userId, String planId, int versionNo) {
        return view(requirePlan(userId, planId), requireVersion(userId, planId, versionNo));
    }

    @Override
    public List<Map<String, Object>> versions(Long userId, String planId) {
        TripPlan plan = requirePlan(userId, planId);
        return mapper.versions(plan.getId()).stream().map(version -> summary(plan, version)).collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> history(Long userId, int limit) {
        List<Map<String, Object>> items = new ArrayList<>();
        for (TripPlan plan : mapper.history(userId, limit)) {
            TripPlanVersion version = mapper.latestVersion(plan.getId());
            items.add(summary(plan, version));
        }
        return items;
    }

    private Map<String, Object> summary(TripPlan plan, TripPlanVersion version) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("plan_id", plan.getPublicId()); item.put("title", plan.getTitle());
        item.put("version_no", version.getVersionNo()); item.put("city", version.getMainCity());
        item.put("start_date", version.getStartDate()); item.put("end_date", version.getEndDate());
        item.put("travel_days", version.getTravelDays()); item.put("total_budget", version.getTotalBudget());
        item.put("currency", version.getCurrency()); item.put("created_at", version.getCreatedAt());
        return item;
    }

    private PlanVersionVO view(TripPlan plan, TripPlanVersion version) {
        PlanVersionVO view = new PlanVersionVO(); view.setPlanId(plan.getPublicId());
        view.setVersionNo(version.getVersionNo()); view.setData(json.read(version.getPlanJson(), TripPlanVO.class));
        return view;
    }

    /** 定期终止中断或超时任务；晚返回的模型结果不能覆盖终态。 */
    @Scheduled(fixedDelayString = "${tripstar.task.recovery-delay-ms:60000}")
    public void expireTasks() {
        try { mapper.expireTasks(); }
        catch (Exception e) { log.warn("任务恢复检查失败，异常类型：{}", e.getClass().getSimpleName()); }
    }
}
