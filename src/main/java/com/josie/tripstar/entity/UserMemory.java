package com.josie.tripstar.entity;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;

/** 长期旅行偏好；独立维护衰减时间，避免重复读取导致累计衰减。 */
@Data
public class UserMemory {
    private Long id;
    private String publicId;
    private Long userId;
    private String content;
    private byte[] contentHash;
    private String source;
    private BigDecimal weight;
    private LocalDateTime lastDecayAt;
    private LocalDateTime lastAccessAt;
    private Long lockVersion;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
