package com.josie.tripstar.entity;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;

/** 绑定行程版本的会话；nextSequence 用于并发冲突检查。 */
@Data
public class ChatSession {
    private Long id;
    private String publicId;
    private Long planVersionId;
    private Long nextSequence;
    private LocalDateTime createdAt;
}
