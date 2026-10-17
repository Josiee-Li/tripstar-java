package com.josie.tripstar.entity.vo;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class PlanVersionVO {
    private boolean success = true;
    private String message = "旅行计划生成成功";
    private String planId;
    private Integer versionNo;
    private TripPlanVO data;
}
