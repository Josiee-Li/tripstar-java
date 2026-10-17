package com.josie.tripstar.service;

import com.josie.tripstar.entity.vo.TripRequest;
import com.josie.tripstar.entity.vo.TripPlanVO;

/** 旅行需求规范化、行程生成与结构校验接口。 */
public interface TripPlannerService {
    /** 校验并规范化城市、日期及停留天数。 */
    void normalize(TripRequest request);

    TripPlanVO generate(Long userId, TripRequest request);

    /** 解析模型输出并校验每日安排及预算汇总。 */
    TripPlanVO parseAndValidate(String output, TripRequest request);
}
