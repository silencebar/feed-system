package com.example.feedsystem.feed.mapper;

import com.example.feedsystem.video.model.VideoDO;
import java.time.OffsetDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface FeedMapper {
    List<VideoDO> selectLatest(@Param("before") OffsetDateTime before, @Param("limit") int limit);
    List<VideoDO> selectByLikesCount(@Param("likesBefore") Long likesBefore,
            @Param("idBefore") Long idBefore, @Param("limit") int limit);
    List<VideoDO> selectByFollowing(@Param("accountId") Long accountId,
            @Param("before") OffsetDateTime before, @Param("limit") int limit);
    List<VideoDO> selectByPopularity(@Param("popularityBefore") Long popularityBefore,
            @Param("timeBefore") OffsetDateTime timeBefore, @Param("idBefore") Long idBefore,
            @Param("limit") int limit);
    List<VideoDO> selectByTag(@Param("tagName") String tagName, @Param("limit") int limit);
    List<VideoDO> selectByIds(@Param("ids") List<Long> ids);
    List<Long> selectLikedVideoIds(@Param("accountId") Long accountId, @Param("ids") List<Long> ids);
}
