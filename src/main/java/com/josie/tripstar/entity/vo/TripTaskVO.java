package com.josie.tripstar.entity.vo;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TripTaskVO {
    private String taskId;
    private String planId;
    private String status;
    private String stage;
    private Integer progress;
    private String message;
    private String error;
    private String errorCode;
    private PlanVersionVO result;
}
