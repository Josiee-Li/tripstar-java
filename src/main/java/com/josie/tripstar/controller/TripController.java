package com.josie.tripstar.controller;

import com.josie.tripstar.entity.result.ApiResult;
import com.josie.tripstar.entity.vo.*;
import com.josie.tripstar.interceptor.UserInterceptor;
import com.josie.tripstar.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import javax.validation.Valid;
import javax.validation.constraints.*;
import java.util.*;

/** MVC 控制层只接收、校验参数并委托业务，不直接访问数据库。 */
@Validated
@RestController
@RequestMapping("/api/trip")
@RequiredArgsConstructor
public class TripController {
    private final TripService service;
    private final UserService users;

    @PostMapping("/plan")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public TripTaskVO plan(@RequestAttribute(UserInterceptor.USER_ATTRIBUTE) String user, @Valid @RequestBody TripRequest request) {
        return service.submit(users.getOrCreate(user).getId(), request);
    }

    @GetMapping("/status/{taskId}")
    public TripTaskVO status(@RequestAttribute(UserInterceptor.USER_ATTRIBUTE) String user, @PathVariable String taskId) {
        return service.status(users.getOrCreate(user).getId(), taskId);
    }

    @GetMapping("/history")
    public Map<String, Object> history(@RequestAttribute(UserInterceptor.USER_ATTRIBUTE) String user,
                                     @RequestParam(defaultValue = "10") @Min(1) @Max(50) int limit) {
        return Collections.singletonMap("items", service.history(users.getOrCreate(user).getId(), limit));
    }

    @GetMapping("/plans/{planId}/versions")
    public ApiResult<?> versions(@RequestAttribute(UserInterceptor.USER_ATTRIBUTE) String user, @PathVariable String planId) {
        return ApiResult.ok(service.versions(users.getOrCreate(user).getId(), planId));
    }

    @GetMapping("/plans/{planId}/versions/{versionNo}")
    public PlanVersionVO version(@RequestAttribute(UserInterceptor.USER_ATTRIBUTE) String user,
                                 @PathVariable String planId, @PathVariable @Min(1) int versionNo) {
        return service.version(users.getOrCreate(user).getId(), planId, versionNo);
    }
}
