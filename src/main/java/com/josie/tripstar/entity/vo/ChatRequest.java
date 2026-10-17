package com.josie.tripstar.entity.vo;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;
import javax.validation.constraints.*;

/**
 * 客户端仅提交会话 ID 和问题；
 * 服务端校验会话归属，并读取绑定的行程版本及聊天历史。
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
/**
 * 这是 Jackson 的 JSON 字段命名注解，让 Java 的驼峰属性在 JSON 中使用下划线命名。
 *接收请求和返回响应都生效：接收时将 start_date 转为 startDate，返回时反向转换。注解只影响所标注的类，不修改 Java 属性名。
 */
public class ChatRequest {
    @NotBlank @Size(max = 64) private String sessionId;
    @NotBlank @Size(max = 4000) private String message;
}
