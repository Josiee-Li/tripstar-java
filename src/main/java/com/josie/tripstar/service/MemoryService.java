// SPDX-License-Identifier: GPL-2.0-only
// 来源与迁移说明见 THIRD_PARTY_NOTICES.md。
package com.josie.tripstar.service;

import com.josie.tripstar.entity.UserMemory;
import java.math.BigDecimal;
import java.util.List;

/** 用户长期旅行偏好的存储与召回接口。 */
public interface MemoryService {
    /** 返回权重衰减后仍有效的 Top-K 偏好。 */
    List<UserMemory> recall(Long userId);

    String prompt(Long userId);

    /** 新增偏好，相同内容合并权重。 */
    void add(Long userId, String content, String source, BigDecimal weight);
}
