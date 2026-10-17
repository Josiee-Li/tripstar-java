// SPDX-License-Identifier: GPL-2.0-only
// 来源与迁移说明见 THIRD_PARTY_NOTICES.md。
package com.josie.tripstar.service;

import com.josie.tripstar.entity.vo.TripRequest;

/** 用户长期旅行偏好提取接口。 */
public interface PreferenceExtractor {
    /** 可选提取步骤，失败不影响已完成的旅行规划。 */
    void extract(Long userId, TripRequest request);
}
