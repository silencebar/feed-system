package com.example.feedsystem.like.mapper;

import com.example.feedsystem.video.model.VideoDO;
import java.time.OffsetDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface LikeMapper {

    boolean existsVideoById(Long videoId);

    boolean existsByVideoIdAndAccountId(@Param("videoId") Long videoId, @Param("accountId") Long accountId);

    int insertLike(
            @Param("videoId") Long videoId,
            @Param("accountId") Long accountId,
            @Param("createdAt") OffsetDateTime createdAt
    );

    int deleteByVideoIdAndAccountId(@Param("videoId") Long videoId, @Param("accountId") Long accountId);

    int changeVideoStats(
            @Param("videoId") Long videoId,
            @Param("likesChange") int likesChange,
            @Param("popularityChange") int popularityChange
    );

    List<VideoDO> selectLikedVideosByAccountId(@Param("accountId") Long accountId, @Param("limit") int limit);
}
