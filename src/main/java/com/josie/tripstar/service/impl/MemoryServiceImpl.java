// SPDX-License-Identifier: GPL-2.0-only
// Java 迁移来源：1sdv/TripStar backend/app/memory；许可与改动说明见 THIRD_PARTY_NOTICES.md。
package com.josie.tripstar.service.impl;

import com.josie.tripstar.service.MemoryService;
import com.josie.tripstar.utils.MemoryPolicy;

import com.josie.tripstar.mapper.MemoryMapper;
import com.josie.tripstar.entity.UserMemory;
import com.josie.tripstar.entity.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

/** 独立源码实现的记忆管理：MySQL 去重、惰性衰减、乐观锁与 Top-K 召回。 */
@Service
@RequiredArgsConstructor
public class MemoryServiceImpl implements MemoryService {
    private final MemoryMapper mapper;
    @Value("${tripstar.memory.enabled:true}") private boolean enabled;

    @Override
    public List<UserMemory> recall(Long userId) {
        if (!enabled) return Collections.emptyList();
        List<UserMemory> candidates = mapper.list(userId);
        List<UserMemory> valid = new ArrayList<>();
        for (UserMemory initial : candidates) {
            UserMemory item = initial;
            for (int attempt = 0; attempt < 3; attempt++) {
                if (item == null) break;
                LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
                item.setWeight(MemoryPolicy.decay(item.getWeight(), item.getLastDecayAt(), now));
                item.setLastDecayAt(now);
                if (item.getWeight().compareTo(new BigDecimal("2")) >= 0) item.setLastAccessAt(now);
                if (mapper.update(item) == 1) {
                    if (item.getWeight().compareTo(new BigDecimal("2")) >= 0) valid.add(item);
                    break;
                }
                item = mapper.byHash(userId, initial.getContentHash());
            }
        }
        // 低权重仅过滤，保留用户数据；数据库中的衰减结果已持久化。
        return valid.stream().sorted(Comparator.comparing(UserMemory::getWeight).reversed()
                .thenComparing(UserMemory::getId)).limit(10).collect(Collectors.toList());
    }

    @Override
    public String prompt(Long userId) {
        return recall(userId).stream().map(item -> "- " + item.getContent()).collect(Collectors.joining("\n"));
    }

    @Override
    public void add(Long userId, String content, String source, BigDecimal weight) {
        if (!enabled) return;
        if (weight == null || weight.compareTo(new BigDecimal("4")) < 0 || weight.compareTo(BigDecimal.TEN) > 0
                || !("explicit".equals(source) || "implicit".equals(source))) return;
        String text = content == null ? "" : content.trim();
        if (text.isEmpty() || text.length() > 120) return;
        byte[] hash = hash(text);
        UserMemory fresh = new UserMemory();
        fresh.setPublicId(UUID.randomUUID().toString()); fresh.setUserId(userId);
        fresh.setContent(text); fresh.setContentHash(hash); fresh.setSource(source); fresh.setWeight(weight);
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        fresh.setLastDecayAt(now); fresh.setLastAccessAt(now);
        mapper.insertIfAbsent(fresh);
        UserMemory item = mapper.byHash(userId, hash);
        if (item != null && fresh.getPublicId().equals(item.getPublicId())) return;
        // 唯一索引处理并发插入，版本号处理并发合并，重试次数有限。
        for (int attempt = 0; attempt < 3; attempt++) {
            if (item == null) break;
            now = LocalDateTime.now(ZoneOffset.UTC);
            item.setWeight(MemoryPolicy.merge(MemoryPolicy.decay(item.getWeight(), item.getLastDecayAt(), now), weight));
            item.setLastDecayAt(now); item.setLastAccessAt(now);
            if ("explicit".equals(source)) item.setSource(source);
            if (mapper.update(item) == 1) return;
            item = mapper.byHash(userId, hash);
        }
        throw BusinessException.conflict("偏好正在被其他请求更新，请重试");
    }

    private byte[] hash(String text) {
        try { return MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException("运行环境不支持 SHA-256", e); }
    }
}
