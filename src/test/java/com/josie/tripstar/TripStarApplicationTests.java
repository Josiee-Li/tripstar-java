package com.josie.tripstar;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.josie.tripstar.entity.exception.BusinessException;
import com.josie.tripstar.service.*;
import com.josie.tripstar.entity.vo.TripRequest;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** 使用隔离的 H2 数据库与模型替身验证全链路，不访问本地 MySQL 或真实模型。 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TripStarApplicationTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired JdbcTemplate jdbc;
    @Autowired com.josie.tripstar.mapper.TripMapper trips;
    @Autowired TripPlannerService planner;
    @MockBean ModelService model;
    private String user;
    private String planJson;

    @BeforeEach
    void prepare() throws Exception {
        user = UUID.randomUUID().toString();
        planJson = org.springframework.util.StreamUtils.copyToString(
                new ClassPathResource("plan-response.json").getInputStream(), StandardCharsets.UTF_8);
        when(model.complete(anyList(), anyInt())).thenReturn(planJson);
    }

    @Test
    void contextLoads() { assertThat(planner).isNotNull(); }

    private String request(String key, String planId) {
        return "{\"city\":\"北京\",\"start_date\":\"2026-11-01\",\"end_date\":\"2026-11-01\","
                + "\"travel_days\":1,\"transportation\":\"公共交通\",\"accommodation\":\"当日返程\","
                + "\"idempotency_key\":\"" + key + "\"" + (planId == null ? "" : ",\"plan_id\":\"" + planId + "\"") + "}";
    }

    private JsonNode submit(String payload) throws Exception {
        return mapper.readTree(mvc.perform(post("/api/trip/plan").header("X-User-Id", user)
                .contentType(MediaType.APPLICATION_JSON).content(payload))
                .andDo(result -> {
                    if (result.getResolvedException() != null) {
                        throw new AssertionError("规划请求处理失败", result.getResolvedException());
                    }
                }).andExpect(status().isAccepted())
                .andReturn().getResponse().getContentAsString());
    }

    private JsonNode task(String taskId) throws Exception {
        return mapper.readTree(mvc.perform(get("/api/trip/status/" + taskId).header("X-User-Id", user))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }

    private JsonNode completed() throws Exception {
        JsonNode submitted = submit(request("first", null));
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                assertThat(task(submitted.path("task_id").asText()).path("status").asText()).isEqualTo("completed"));
        return task(submitted.path("task_id").asText());
    }

    @Test
    void planPersistsVersionsAndSupportsIdempotentRetry() throws Exception {
        JsonNode first = completed();
        String planId = first.path("plan_id").asText();
        JsonNode retry = submit(request("first", planId));
        assertThat(retry.path("task_id")).isEqualTo(first.path("task_id"));
        assertThat(first.at("/result/data/budget/total").decimalValue()).isEqualByComparingTo("150");
        JsonNode second = submit(request("second", planId));
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                assertThat(task(second.path("task_id").asText()).at("/result/version_no").asInt()).isEqualTo(2));
        mvc.perform(get("/api/trip/history").header("X-User-Id", user)).andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].version_no").value(2));
        mvc.perform(get("/api/trip/plans/" + planId + "/versions/1").header("X-User-Id", user))
                .andExpect(status().isOk()).andExpect(jsonPath("$.version_no").value(1));
        mvc.perform(post("/api/trip/plan").header("X-User-Id", user).contentType(MediaType.APPLICATION_JSON)
                .content(request("first", planId).replace("公共交通", "自驾"))).andExpect(status().isConflict());
    }

    @Test
    void chatUsesOwnedVersionAndPersistsCompleteTurns() throws Exception {
        JsonNode first = completed();
        String planId = first.path("plan_id").asText();
        JsonNode created = mapper.readTree(mvc.perform(post("/api/chat/sessions").header("X-User-Id", user)
                .contentType(MediaType.APPLICATION_JSON).content("{\"plan_id\":\"" + planId + "\",\"version_no\":1}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        String sessionId = created.at("/data/session_id").asText();
        when(model.complete(anyList(), eq(1500))).thenReturn("预计费用150元，请以实际价格为准。");
        String body = "{\"session_id\":\"" + sessionId + "\",\"message\":\"预算多少？\"}";
        mvc.perform(post("/api/chat/ask").header("X-User-Id", user).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.sequence_no").value(2));
        mvc.perform(get("/api/chat/sessions/" + sessionId + "/messages").header("X-User-Id", user))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].role").value("user"));
        when(model.complete(anyList(), eq(1500))).thenThrow(new BusinessException(
                org.springframework.http.HttpStatus.BAD_GATEWAY, "AI_UNAVAILABLE", "模型暂不可用"));
        mvc.perform(post("/api/chat/ask").header("X-User-Id", user).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadGateway());
        mvc.perform(get("/api/chat/sessions/" + sessionId + "/messages").header("X-User-Id", user))
                .andExpect(jsonPath("$.data.length()").value(2));
        mvc.perform(post("/api/chat/ask").header("X-User-Id", "other-" + user)
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isNotFound());
        mvc.perform(get("/api/trip/status/" + first.path("task_id").asText()).header("X-User-Id", "other-" + user))
                .andExpect(status().isNotFound());
    }

    @Test
    void memoryDeduplicatesTrimmedContentAndIsolatesUsers() throws Exception {
        for (String content : new String[]{"偏爱博物馆", "  偏爱博物馆  "}) {
            mvc.perform(post("/api/memory").header("X-User-Id", user).contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(java.util.Collections.singletonMap("content", content))))
                    .andExpect(status().isOk());
        }
        mvc.perform(get("/api/memory").header("X-User-Id", user)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1)).andExpect(jsonPath("$.data[0].weight").value(10.0));
        mvc.perform(get("/api/memory").header("X-User-Id", "other-" + user))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void rejectsInvalidInputBeforeCallingModel() throws Exception {
        mvc.perform(post("/api/trip/plan").contentType(MediaType.APPLICATION_JSON).content(request("bad", null)))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/trip/plan").header("X-User-Id", user).contentType(MediaType.APPLICATION_JSON)
                .content(request("bad", null).replace("\"travel_days\":1", "\"travel_days\":2")))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/trip/history?limit=1000").header("X-User-Id", user)).andExpect(status().isBadRequest());
        verifyNoInteractions(model);
    }

    @Test
    void invalidGeneratedBudgetFailsTaskWithoutSavingVersion() throws Exception {
        when(model.complete(anyList(), anyInt())).thenReturn(planJson.replace("\"total\": 150", "\"total\": 151"));
        JsonNode submitted = submit(request("bad-model", null));
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                assertThat(task(submitted.path("task_id").asText()).path("status").asText()).isEqualTo("failed"));
        assertThat(task(submitted.path("task_id").asText()).path("error_code").asText()).isEqualTo("INVALID_MODEL_RESPONSE");
        mvc.perform(get("/api/trip/plans/" + submitted.path("plan_id").asText() + "/versions").header("X-User-Id", user))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void validatesDailyDatesCityAllocationAndNumericMoney() throws Exception {
        TripRequest request = mapper.readValue(request("validation", null), TripRequest.class);
        planner.normalize(request);
        assertThat(planner.parseAndValidate(planJson, request).getDays()).hasSize(1);
        ObjectNode wrongDate = (ObjectNode) mapper.readTree(planJson);
        ((ObjectNode) wrongDate.withArray("days").get(0)).put("date", "2026-11-02");
        assertThatThrownBy(() -> planner.parseAndValidate(wrongDate.toString(), request)).isInstanceOf(BusinessException.class);
        ObjectNode wrongCity = (ObjectNode) mapper.readTree(planJson);
        ((ObjectNode) wrongCity.withArray("days").get(0)).put("city", "上海");
        assertThatThrownBy(() -> planner.parseAndValidate(wrongCity.toString(), request)).isInstanceOf(BusinessException.class);
        ObjectNode wrongMoney = (ObjectNode) mapper.readTree(planJson);
        ((ObjectNode) wrongMoney.get("budget")).put("total", "150元");
        assertThatThrownBy(() -> planner.parseAndValidate(wrongMoney.toString(), request)).isInstanceOf(BusinessException.class);
    }

    @Test
    void concurrentChatCommitsOnlyOneCompleteTurn() throws Exception {
        JsonNode first = completed();
        JsonNode created = mapper.readTree(mvc.perform(post("/api/chat/sessions").header("X-User-Id", user)
                .contentType(MediaType.APPLICATION_JSON).content("{\"plan_id\":\"" + first.path("plan_id").asText()
                        + "\",\"version_no\":1}")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        String sessionId = created.at("/data/session_id").asText();
        java.util.concurrent.CountDownLatch entered = new java.util.concurrent.CountDownLatch(2);
        when(model.complete(anyList(), eq(1500))).thenAnswer(invocation -> {
            entered.countDown();
            if (!entered.await(5, java.util.concurrent.TimeUnit.SECONDS)) throw new IllegalStateException("并发测试等待超时");
            return "出行前请核实费用。";
        });
        java.util.concurrent.ExecutorService pool = java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            java.util.concurrent.Callable<Integer> call = () -> mvc.perform(post("/api/chat/ask")
                    .header("X-User-Id", user).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"session_id\":\"" + sessionId + "\",\"message\":\"有什么建议？\"}"))
                    .andReturn().getResponse().getStatus();
            java.util.concurrent.Future<Integer> one = pool.submit(call);
            java.util.concurrent.Future<Integer> two = pool.submit(call);
            assertThat(java.util.Arrays.asList(one.get(10, java.util.concurrent.TimeUnit.SECONDS),
                    two.get(10, java.util.concurrent.TimeUnit.SECONDS))).containsExactlyInAnyOrder(200, 409);
            mvc.perform(get("/api/chat/sessions/" + sessionId + "/messages").header("X-User-Id", user))
                    .andExpect(jsonPath("$.data.length()").value(2))
                    .andExpect(jsonPath("$.data[1].sequence_no").value(2));
        } finally { pool.shutdownNow(); }
    }

    @Test
    void staleTaskExpiresAndCannotBeCompletedAfterwards() throws Exception {
        // 直接插入独立测试库中的中断任务，验证恢复 SQL 和终态保护。
        jdbc.update("INSERT INTO app_user(public_id) VALUES(?)", user);
        Long userId = jdbc.queryForObject("SELECT id FROM app_user WHERE public_id = ?", Long.class, user);
        com.josie.tripstar.entity.TripPlan plan = new com.josie.tripstar.entity.TripPlan();
        plan.setPublicId(java.util.UUID.randomUUID().toString()); plan.setUserId(userId); plan.setTitle("超时测试");
        trips.insertPlan(plan);
        com.josie.tripstar.entity.TripTask task = new com.josie.tripstar.entity.TripTask();
        task.setPublicId(java.util.UUID.randomUUID().toString()); task.setPlanId(plan.getId());
        task.setIdempotencyKey("expired"); task.setRequestJson(request("expired", null)); trips.insertTask(task);
        trips.claimTask(task.getId());
        jdbc.update("UPDATE trip_task SET started_at = TIMESTAMPADD(MINUTE, -11, CURRENT_TIMESTAMP) WHERE id = ?", task.getId());
        assertThat(trips.expireTasks()).isGreaterThanOrEqualTo(1);
        assertThat(trips.taskById(task.getId()).getErrorCode()).isEqualTo("TASK_EXPIRED");
        assertThat(trips.completeTask(task.getId())).isZero();
        assertThat(trips.progress(task.getId())).isZero();
    }
}
