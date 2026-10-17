// SPDX-License-Identifier: GPL-2.0-only
// Java 迁移来源：1sdv/TripStar backend/app/memory；许可与改动说明见 THIRD_PARTY_NOTICES.md。
package com.josie.tripstar.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 参考 TripStar memory_manager.py 的权重算法独立迁移为 Java。
 * 日衰减 0.97、准入 4、召回阈值 2、合并增量 0.4、最高权重 10。
 * 使用 last_decay_at 而非 last_access_at，读取或合并前先衰减，避免遗忘时间被错误重置。
 */
public final class MemoryPolicy {
    private MemoryPolicy() { }
    public static BigDecimal decay(BigDecimal weight, LocalDateTime from, LocalDateTime now) {
        double days = Math.max(0, Duration.between(from, now).toMillis()) / 86400000.0;
        return BigDecimal.valueOf(weight.doubleValue() * Math.pow(0.97, days)).setScale(6, RoundingMode.HALF_UP);
    }
    public static BigDecimal merge(BigDecimal decayed, BigDecimal incoming) {
        return decayed.add(incoming.multiply(new BigDecimal("0.4"))).min(BigDecimal.TEN).setScale(6, RoundingMode.HALF_UP);
    }
}
