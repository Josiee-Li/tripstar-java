package com.josie.tripstar.entity;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;

/** 持久化规划任务，状态和进度用于前端轮询。 */
@Data
public class TripTask {
    private Long id;
    private String publicId;
    private Long planId;
    private String idempotencyKey;
    private String status;
    private String stage;
    private Integer progress;
    private String requestJson;
    private String message;
    private String errorCode;
    private String errorMessage;
    private Long lockVersion;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
