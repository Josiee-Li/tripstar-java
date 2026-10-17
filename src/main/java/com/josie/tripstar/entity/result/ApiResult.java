package com.josie.tripstar.entity.result;

import lombok.AllArgsConstructor;
import lombok.Getter;
import com.fasterxml.jackson.annotation.JsonInclude;

/** 统一成功/失败响应；HTTP 状态码仍表达请求的实际结果。 */
@Getter
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@com.fasterxml.jackson.databind.annotation.JsonNaming(com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ApiResult<T> {
    private final boolean success;
    private final String message;
    private final String errorCode;
    private final T data;
    public static <T> ApiResult<T> ok(T data) { return new ApiResult<>(true, "操作成功", null, data); }
    public static ApiResult<Void> error(String code, String message) {
        return new ApiResult<>(false, message, code, null);
    }
}
