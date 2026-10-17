package com.josie.tripstar.entity;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;

/** 会话中的用户消息或模型回复。 */
@Data
public class ChatMessage {
    private Long id;
    private Long sessionId;
    private Long sequenceNo;
    private String role;
    private String content;
    private LocalDateTime createdAt;
}
