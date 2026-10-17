package com.josie.tripstar.entity.vo;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;
import javax.validation.Valid;
import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** 经过后端校验的结构化行程，金额使用 BigDecimal 避免浮点误差。 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class TripPlanVO {
    @NotBlank @Size(max = 128) private String city;
    @NotNull @Size(min = 1, max = 30) private List<@NotBlank String> cities;
    @NotNull private LocalDate startDate;
    @NotNull private LocalDate endDate;
    @Valid @NotNull @Size(min = 1, max = 30) private List<DayPlan> days;
    @NotBlank @Size(max = 5000) private String overallSuggestions;
    @Valid @NotNull private Budget budget;
    // 当前版本不接天气数据源，因此不允许模型伪造实时天气。
    private List<Object> weatherInfo = new ArrayList<>();

    @Data
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class DayPlan {
        @NotNull private LocalDate date;
        @NotNull @Min(0) @Max(29) private Integer dayIndex;
        @NotBlank private String city;
        @com.fasterxml.jackson.annotation.JsonProperty("is_transfer_day") private boolean transferDay;
        @Size(max = 1000) private String transferInfo;
        @NotBlank @Size(max = 3000) private String description;
        @NotBlank @Size(max = 1000) private String transportation;
        @NotBlank @Size(max = 1000) private String accommodation;
        @Valid private Hotel hotel;
        @Valid @NotNull @Size(min = 1, max = 8) private List<Attraction> attractions;
        @Valid @NotNull @Size(min = 3, max = 5) private List<Meal> meals;
    }

    @Data
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class Attraction {
        @NotBlank @Size(max = 255) private String name;
        @Size(max = 500) private String address;
        @NotNull @Min(1) @Max(1440) private Integer visitDuration;
        @NotBlank @Size(max = 2000) private String description;
        @Size(max = 100) private String category;
        @NotNull @DecimalMin("0") @DecimalMax("9999999") @Digits(integer = 7, fraction = 2) private BigDecimal ticketPrice;
        private boolean reservationRequired;
        @Size(max = 1000) private String reservationTips;
    }

    @Data
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class Meal {
        @NotBlank @Pattern(regexp = "breakfast|lunch|dinner|snack") private String type;
        @NotBlank @Size(max = 255) private String name;
        @Size(max = 500) private String description;
        @NotNull @DecimalMin("0") @DecimalMax("9999999") @Digits(integer = 7, fraction = 2) private BigDecimal estimatedCost;
    }

    @Data
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class Hotel {
        @NotBlank @Size(max = 255) private String name;
        @Size(max = 500) private String address;
        @Size(max = 100) private String priceRange;
        @Size(max = 100) private String type;
        @NotNull @DecimalMin("0") @DecimalMax("9999999") @Digits(integer = 7, fraction = 2) private BigDecimal estimatedCost;
    }

    @Data
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class Budget {
        @NotNull @DecimalMin("0") @Digits(integer = 10, fraction = 2) private BigDecimal totalAttractions;
        @NotNull @DecimalMin("0") @Digits(integer = 10, fraction = 2) private BigDecimal totalHotels;
        @NotNull @DecimalMin("0") @Digits(integer = 10, fraction = 2) private BigDecimal totalMeals;
        @NotNull @DecimalMin("0") @Digits(integer = 10, fraction = 2) private BigDecimal totalTransportation;
        @NotNull @DecimalMin("0") @Digits(integer = 10, fraction = 2) private BigDecimal totalInterCityTransport;
        @NotNull @DecimalMin("0") @Digits(integer = 10, fraction = 2) private BigDecimal total;
    }
}
