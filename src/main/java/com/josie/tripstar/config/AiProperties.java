package com.josie.tripstar.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import javax.validation.constraints.*;

/** 通过环境变量配置模型，不把密钥写入源码。 */
@Data
@Component
@Validated
@ConfigurationProperties(prefix = "tripstar.ai")
public class AiProperties {
    @NotBlank private String baseUrl = "https://api.deepseek.com/v1";
    private String apiKey = "";
    @NotBlank private String model = "deepseek-chat";
    @Min(512) @Max(65536) private int planMaxTokens = 8192;
    @Min(1000) @Max(300000) private int readTimeoutMs = 120000;
    @Min(1000) @Max(30000) private int connectTimeoutMs = 10000;
}
