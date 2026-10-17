package com.josie.tripstar.mapper;

import com.josie.tripstar.entity.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/** 仅负责数据库访问，SQL 位于 resources/mapper。 */
@Mapper
public interface TripMapper {
    void insertPlan(TripPlan plan);
    TripPlan findPlan(@Param("publicId") String publicId, @Param("userId") Long userId);
    TripPlan findPlanById(@Param("id") Long id);
    TripPlanVersion latestVersion(@Param("planId") Long planId);
    TripPlan lockPlan(@Param("id") Long id);
    List<TripPlan> history(@Param("userId") Long userId, @Param("limit") int limit);
    void insertTask(TripTask task);
    TripTask taskByKey(@Param("planId") Long planId, @Param("key") String key);
    TripTask findTask(@Param("publicId") String publicId, @Param("userId") Long userId);
    TripTask taskById(@Param("id") Long id);
    TripTask lockTask(@Param("id") Long id);
    int claimTask(@Param("id") Long id);
    int progress(@Param("id") Long id);
    int completeTask(@Param("id") Long id);
    int failTask(@Param("id") Long id, @Param("code") String code, @Param("message") String message);
    int expireTasks();
    int nextVersion(@Param("planId") Long planId);
    void insertVersion(TripPlanVersion version);
    TripPlanVersion version(@Param("planId") Long planId, @Param("versionNo") Integer versionNo);
    TripPlanVersion versionByTask(@Param("taskId") Long taskId);
    TripPlanVersion versionById(@Param("id") Long id);
    List<TripPlanVersion> versions(@Param("planId") Long planId);
}
