package com.josie.tripstar.entity.vo;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;
import javax.validation.constraints.*;

@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class CreateSessionRequest {
    @NotBlank @Size(max = 64) private String planId;
    @NotNull @Min(1) private Integer versionNo;
}
