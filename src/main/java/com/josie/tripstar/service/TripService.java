package com.josie.tripstar.service;

import com.josie.tripstar.entity.TripPlan;
import com.josie.tripstar.entity.TripPlanVersion;
import com.josie.tripstar.entity.vo.*;
import java.util.List;
import java.util.Map;

/** 旅行规划任务、行程版本与历史查询接口。 */
public interface TripService {
    /** 提交任务，同一计划内按幂等键去重。 */
    TripTaskVO submit(Long userId, TripRequest request);

    /** 查询当前用户所属任务的执行状态。 */
    TripTaskVO status(Long userId, String taskId);

    /** 查询计划并校验用户归属，不存在时抛出业务异常。 */
    TripPlan requirePlan(Long userId, String publicId);

    /** 查询具体版本并校验所属计划的用户归属。 */
    TripPlanVersion requireVersion(Long userId, String publicId, int versionNo);

    PlanVersionVO version(Long userId, String planId, int versionNo);

    List<Map<String, Object>> versions(Long userId, String planId);

    List<Map<String, Object>> history(Long userId, int limit);
}
