package com.josie.tripstar.entity;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;

/** 旅行计划主记录；具体行程保存在不可变版本中。 */
@Data
public class TripPlan {
    private Long id;
    private String publicId;
    private Long userId;
    private String title;
    private LocalDateTime createdAt;
}
