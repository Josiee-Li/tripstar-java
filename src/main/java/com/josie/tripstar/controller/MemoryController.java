package com.josie.tripstar.controller;

import com.josie.tripstar.entity.UserMemory;
import com.josie.tripstar.entity.result.ApiResult;
import com.josie.tripstar.entity.vo.MemoryRequest;
import com.josie.tripstar.interceptor.UserInterceptor;
import com.josie.tripstar.service.UserService;
import com.josie.tripstar.service.MemoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import javax.validation.Valid;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/** 显式偏好的写入与召回，不暴露内部主键和内容哈希。 */
@RestController
@RequestMapping("/api/memory")
@RequiredArgsConstructor
public class MemoryController {
    private final MemoryService service;
    private final UserService users;

    @PostMapping
    public ApiResult<?> add(@RequestAttribute(UserInterceptor.USER_ATTRIBUTE) String user,
                             @Valid @RequestBody MemoryRequest request) {
        service.add(users.getOrCreate(user).getId(), request.getContent(), "explicit", new BigDecimal("8"));
        return ApiResult.ok(null);
    }

    @GetMapping
    public ApiResult<?> list(@RequestAttribute(UserInterceptor.USER_ATTRIBUTE) String user) {
        List<Map<String, Object>> items = service.recall(users.getOrCreate(user).getId()).stream().map(this::view)
                .collect(Collectors.toList());
        return ApiResult.ok(items);
    }

    private Map<String, Object> view(UserMemory item) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("memory_id", item.getPublicId()); result.put("content", item.getContent());
        result.put("source", item.getSource()); result.put("weight", item.getWeight()); return result;
    }
}
