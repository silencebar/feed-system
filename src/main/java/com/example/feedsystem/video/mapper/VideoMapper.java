package com.example.feedsystem.video.mapper;

import com.example.feedsystem.video.model.OutboxMsgDO;
import com.example.feedsystem.video.model.TagDO;
import com.example.feedsystem.video.model.VideoDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface VideoMapper {

    int insertVideo(VideoDO video);

    int deleteById(Long id);

    VideoDO selectById(Long id);

    List<VideoDO> selectByAuthorId(@Param("authorId") Long authorId, @Param("limit") int limit);

    int insertOutbox(OutboxMsgDO outboxMsg);

    TagDO selectTagByName(String name);

    int insertTag(TagDO tag);

    int insertVideoTag(@Param("videoId") Long videoId, @Param("tagId") Long tagId);
}
