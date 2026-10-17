package com.josie.tripstar.service.impl;

import com.josie.tripstar.service.ModelService;

import com.josie.tripstar.config.AiProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.josie.tripstar.entity.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;
import java.util.*;

/** 普通 HTTP 模型调用，不依赖 Agent、工具调用循环或 HelloAgents 包。 */
@Service
@RequiredArgsConstructor
public class ModelServiceImpl implements ModelService {
    private final RestTemplate modelRestTemplate;
    private final AiProperties properties;

    @Override
    public String complete(List<Map<String, String>> messages, int maxTokens) {
        if (properties.getApiKey() == null || properties.getApiKey().trim().isEmpty()) {
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "AI_NOT_CONFIGURED", "请先配置 LLM_API_KEY");
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", properties.getModel());
        body.put("messages", messages);
        body.put("temperature", 0.3);
        body.put("max_tokens", maxTokens);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(properties.getApiKey().trim());
        try {
            String url = properties.getBaseUrl().replaceAll("/+$", "") + "/chat/completions";
            JsonNode response = modelRestTemplate.postForObject(url, new HttpEntity<>(body, headers), JsonNode.class);
            JsonNode choice = response == null ? null : response.path("choices").path(0);
            if (choice == null || !choice.path("message").path("content").isTextual()
                    || "length".equals(choice.path("finish_reason").asText())) throw invalid();
            String text = choice.path("message").path("content").asText().trim();
            if (text.isEmpty() || text.length() > 250000) throw invalid();
            return text;
        } catch (RestClientException e) {
            // 不记录异常正文：供应商错误可能回显请求内容或鉴权信息。
            throw new BusinessException(HttpStatus.BAD_GATEWAY, "AI_UNAVAILABLE", "模型服务请求失败或超时，请稍后重试");
        }
    }
    private BusinessException invalid() {
        return new BusinessException(HttpStatus.BAD_GATEWAY, "INVALID_MODEL_RESPONSE", "模型返回空内容、截断内容或无效响应");
    }
}
