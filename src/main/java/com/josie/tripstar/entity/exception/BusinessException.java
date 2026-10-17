package com.josie.tripstar.entity.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/** 可安全返回给客户端的业务异常，不包含数据库或模型供应商原始响应。 */
@Getter
public class BusinessException extends RuntimeException {
    private final HttpStatus status;
    private final String code;
    public BusinessException(HttpStatus status, String code, String message) {
        super(message); this.status = status; this.code = code;
    }
    public static BusinessException badRequest(String message) {
        return new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", message);
    }
    public static BusinessException notFound() {
        return new BusinessException(HttpStatus.NOT_FOUND, "NOT_FOUND", "资源不存在或不属于当前用户");
    }
    public static BusinessException conflict(String message) {
        return new BusinessException(HttpStatus.CONFLICT, "CONFLICT", message);
    }
}
