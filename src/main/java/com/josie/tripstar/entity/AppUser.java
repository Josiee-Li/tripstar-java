package com.josie.tripstar.entity;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;

/** 匿名用户，publicId 为客户端标识，不具备登录认证能力。 */
@Data
public class AppUser {
    private Long id;
    private String publicId;
    private String displayName;
    private LocalDateTime createdAt;
}
