package com.josie.tripstar;

import com.josie.tripstar.config.AiProperties;
import com.josie.tripstar.entity.exception.BusinessException;
import com.josie.tripstar.service.ModelService;
import com.josie.tripstar.service.impl.ModelServiceImpl;
import com.josie.tripstar.utils.ModelMessages;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import java.util.Collections;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

/** 验证实际 HTTP 适配层的协议和错误处理，不发送真实网络请求。 */
class ModelServiceTests {
    private AiProperties properties;
    private ModelService service;
    private MockRestServiceServer server;

    @BeforeEach
    void setup() {
        properties = new AiProperties(); properties.setApiKey("test-secret");
        properties.setBaseUrl("https://model.example/v1/");
        RestTemplate template = new RestTemplate();
        server = MockRestServiceServer.bindTo(template).build();
        service = new ModelServiceImpl(template, properties);
    }

    @Test
    void sendsCompatibleRequestAndExtractsReply() {
        server.expect(requestTo("https://model.example/v1/chat/completions"))
                .andExpect(method(HttpMethod.POST)).andExpect(header("Authorization", "Bearer test-secret"))
                .andExpect(jsonPath("$.messages[0].content").value("hello"))
                .andExpect(jsonPath("$.max_tokens").value(1500))
                .andRespond(withSuccess("{\"choices\":[{\"finish_reason\":\"stop\",\"message\":{\"content\":\" answer \"}}]}", MediaType.APPLICATION_JSON));
        assertThat(service.complete(Collections.singletonList(ModelMessages.message("user", "hello")), 1500)).isEqualTo("answer");
        server.verify();
    }

    @Test
    void hidesProviderErrors() {
        server.expect(requestTo("https://model.example/v1/chat/completions"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED).body("provider echoed secret and private content"));
        assertThatThrownBy(() -> service.complete(Collections.emptyList(), 1500)).isInstanceOfSatisfying(
                BusinessException.class, e -> {
                    assertThat(e.getCode()).isEqualTo("AI_UNAVAILABLE");
                    assertThat(e.getMessage()).doesNotContain("secret", "private", "provider");
                });
        server.verify();
    }

    @Test
    void rejectsTruncatedAndUnconfiguredResponses() {
        server.expect(requestTo("https://model.example/v1/chat/completions"))
                .andRespond(withSuccess("{\"choices\":[{\"finish_reason\":\"length\",\"message\":{\"content\":\"partial\"}}]}", MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> service.complete(Collections.emptyList(), 1500)).isInstanceOfSatisfying(
                BusinessException.class, e -> assertThat(e.getCode()).isEqualTo("INVALID_MODEL_RESPONSE"));
        properties.setApiKey("");
        assertThatThrownBy(() -> service.complete(Collections.emptyList(), 1500)).isInstanceOfSatisfying(
                BusinessException.class, e -> assertThat(e.getCode()).isEqualTo("AI_NOT_CONFIGURED"));
        server.verify();
    }
}
