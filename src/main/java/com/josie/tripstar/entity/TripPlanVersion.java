package com.josie.tripstar.entity;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;

/** 一次规划成功对应一个版本，历史聊天绑定具体版本。 */
@Data
public class TripPlanVersion {
    private Long id;
    private Long planId;
    private Long taskId;
    private Integer versionNo;
    private String mainCity;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer travelDays;
    private String language;
    private BigDecimal totalBudget;
    private String currency;
    private String planJson;
    private Integer schemaVersion;
    private LocalDateTime createdAt;
}
