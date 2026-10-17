package com.josie.tripstar.service;

import java.util.List;
import java.util.Map;

/** 模型调用接口，屏蔽具体 HTTP 协议实现。 */
public interface ModelService {
    /** 生成文本回复；模型不可用或响应无效时抛出业务异常。 */
    String complete(List<Map<String, String>> messages, int maxTokens);
}
