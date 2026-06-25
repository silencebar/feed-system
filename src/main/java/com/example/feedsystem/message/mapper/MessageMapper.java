package com.example.feedsystem.message.mapper;

import com.example.feedsystem.message.model.MessageDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MessageMapper {
    int insert(MessageDO message);
    List<MessageDO> selectConversation(@Param("currentUserId") Long currentUserId,
            @Param("peerId") Long peerId, @Param("limit") int limit);
}
