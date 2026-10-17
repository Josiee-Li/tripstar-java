package com.josie.tripstar.utils;

import java.util.LinkedHashMap;
import java.util.Map;

/** 组装模型消息，避免在模型调用接口中放置工具实现。 */
public final class ModelMessages {
    private ModelMessages() { }

    public static Map<String, String> message(String role, String content) {
        Map<String, String> item = new LinkedHashMap<>();
        item.put("role", role);
        item.put("content", content);
        return item;
    }
}
