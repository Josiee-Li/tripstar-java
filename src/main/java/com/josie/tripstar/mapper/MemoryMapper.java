package com.josie.tripstar.mapper;

import com.josie.tripstar.entity.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/** 仅负责数据库访问，SQL 位于 resources/mapper。 */
@Mapper
public interface MemoryMapper {
    List<UserMemory> list(@Param("userId") Long userId);
    UserMemory byHash(@Param("userId") Long userId, @Param("hash") byte[] hash);
    int insertIfAbsent(UserMemory memory);
    int update(UserMemory memory);
}
