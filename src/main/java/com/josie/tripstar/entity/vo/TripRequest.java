package com.josie.tripstar.entity.vo;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;
import javax.validation.Valid;
import javax.validation.constraints.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** 保留参考项目的 snake_case 字段，增加计划归属与幂等参数。 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class TripRequest {
    @Size(max = 64) private String planId;
    @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{1,64}") private String idempotencyKey;
    @Size(max = 128) private String city;
    @Valid @NotNull @Size(max = 30) private List<CityStay> cities = new ArrayList<>();
    @NotNull private LocalDate startDate;
    @NotNull private LocalDate endDate;
    @NotNull @Min(1) @Max(30) private Integer travelDays;
    @NotBlank @Size(max = 100) private String transportation;
    @NotBlank @Size(max = 100) private String accommodation;
    @NotNull @Size(max = 20) private List<@NotBlank @Size(max = 120) String> preferences = new ArrayList<>();
    @Size(max = 3000) private String freeTextInput = "";
    @NotBlank @Pattern(regexp = "zh|en|ja") private String language = "zh";

    @Data
    public static class CityStay {
        @NotBlank @Size(max = 128) private String city;
        @NotNull @Min(1) @Max(30) private Integer days;
    }
}
