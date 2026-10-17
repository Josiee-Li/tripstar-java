package com.josie.tripstar.controller;

import com.josie.tripstar.entity.result.ApiResult;
import com.josie.tripstar.entity.vo.*;
import com.josie.tripstar.interceptor.UserInterceptor;
import com.josie.tripstar.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import javax.validation.Valid;
import javax.validation.constraints.*;
import java.util.Map;

@Validated
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {
    private final ChatService service;
    private final UserService users;

    @PostMapping("/sessions")
    public ApiResult<?> create(@RequestAttribute(UserInterceptor.USER_ATTRIBUTE) String user,
                               @Valid @RequestBody CreateSessionRequest request) {
        return ApiResult.ok(service.create(users.getOrCreate(user).getId(), request));
    }

    @PostMapping("/ask")
    public Map<String, Object> ask(@RequestAttribute(UserInterceptor.USER_ATTRIBUTE) String user,
                                    @Valid @RequestBody ChatRequest request) {
        return service.ask(users.getOrCreate(user).getId(), request);
    }

    @GetMapping("/sessions/{sessionId}/messages")
    public ApiResult<?> messages(@RequestAttribute(UserInterceptor.USER_ATTRIBUTE) String user, @PathVariable String sessionId,
                                 @RequestParam(defaultValue = "0") @Min(0) long after,
                                 @RequestParam(defaultValue = "50") @Min(1) @Max(100) int limit) {
        return ApiResult.ok(service.messages(users.getOrCreate(user).getId(), sessionId, after, limit));
    }
}
