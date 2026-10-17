package com.josie.tripstar.mapper;

import com.josie.tripstar.entity.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/** 仅负责数据库访问，SQL 位于 resources/mapper。 */
@Mapper
public interface ChatMapper {
    void insertSession(ChatSession session);
    ChatSession findSession(@Param("publicId") String publicId, @Param("userId") Long userId);
    ChatSession lockSession(@Param("id") Long id);
    List<ChatMessage> recentMessages(@Param("sessionId") Long sessionId, @Param("limit") int limit);
    List<ChatMessage> messages(@Param("sessionId") Long sessionId, @Param("after") long after, @Param("limit") int limit);
    void insertMessage(ChatMessage message);
    int advance(@Param("id") Long id, @Param("expected") long expected);
}
