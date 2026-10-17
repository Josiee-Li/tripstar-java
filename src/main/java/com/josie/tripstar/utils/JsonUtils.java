package com.josie.tripstar.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import com.josie.tripstar.entity.exception.BusinessException;
import org.springframework.http.HttpStatus;

/** 集中处理持久化 JSON，模型输出只允许纯 JSON 或完整 Markdown 代码块。 */
@Component
@RequiredArgsConstructor
public class JsonUtils {
    private final ObjectMapper mapper;
    public String write(Object value) {
        try { return mapper.writeValueAsString(value); }
        catch (JsonProcessingException e) { throw new IllegalStateException("JSON 序列化失败", e); }
    }
    public <T> T read(String value, Class<T> type) {
        try { return mapper.readValue(value, type); }
        catch (JsonProcessingException e) { throw new IllegalStateException("已保存的数据格式异常", e); }
    }
    public JsonNode modelJson(String value) {
        String text = value.trim();
        if (text.startsWith("```")) {
            int start = text.indexOf('\n');
            if (start >= 0 && text.endsWith("```")) text = text.substring(start + 1, text.length() - 3).trim();
        }
        try { return mapper.readTree(text); }
        catch (JsonProcessingException e) { throw invalidModel(); }
    }
    public static BusinessException invalidModel() {
        return new BusinessException(HttpStatus.BAD_GATEWAY, "INVALID_MODEL_RESPONSE", "模型返回的行程格式不正确，请重新生成");
    }
}
