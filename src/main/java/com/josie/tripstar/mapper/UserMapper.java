package com.josie.tripstar.mapper;

import com.josie.tripstar.entity.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/** 仅负责数据库访问，SQL 位于 resources/mapper。 */
@Mapper
public interface UserMapper {
    void insertIfAbsent(@Param("publicId") String publicId);
    AppUser find(@Param("publicId") String publicId);
}
