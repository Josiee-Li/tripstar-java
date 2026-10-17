package com.josie.tripstar.interceptor;

import com.josie.tripstar.entity.exception.BusinessException;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** 兼容原项目的匿名用户标识；上线登录功能时应从认证主体中获取身份。 */
@Component
public class UserInterceptor implements HandlerInterceptor {
    public static final String USER_ATTRIBUTE = "tripstar.userId";
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String id = request.getHeader("X-User-Id");
        if (id == null || !id.matches("[A-Za-z0-9_-]{1,64}")) {
            throw BusinessException.badRequest("请提供 X-User-Id 请求头，使用 1~64 位字母、数字、下划线或连字符");
        }
        request.setAttribute(USER_ATTRIBUTE, id);
        return true;
    }
}
