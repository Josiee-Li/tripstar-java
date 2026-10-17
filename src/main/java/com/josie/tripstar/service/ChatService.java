package com.josie.tripstar.service;

import com.josie.tripstar.entity.vo.ChatRequest;
import com.josie.tripstar.entity.vo.CreateSessionRequest;
import java.util.List;
import java.util.Map;

/** 绑定行程版本的会话与问答业务接口。 */
public interface ChatService {
    Map<String, Object> create(Long userId, CreateSessionRequest request);

    /** 基于服务端保存的行程和历史问答，成功后原子保存完整一轮对话。 */
    Map<String, Object> ask(Long userId, ChatRequest request);

    List<Map<String, Object>> messages(Long userId, String sessionId, long after, int limit);
}
