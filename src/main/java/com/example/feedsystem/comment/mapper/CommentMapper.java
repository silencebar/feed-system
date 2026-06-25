package com.example.feedsystem.comment.mapper;

import com.example.feedsystem.comment.model.CommentDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CommentMapper {
    boolean existsVideoById(Long videoId);
    Long selectVideoAuthorId(Long videoId);
    int insertComment(CommentDO comment);
    CommentDO selectById(Long id);
    int deleteById(Long id);
    int increaseVideoPopularity(Long videoId);
    List<CommentDO> selectByVideoId(@Param("videoId") Long videoId, @Param("limit") int limit);
}
