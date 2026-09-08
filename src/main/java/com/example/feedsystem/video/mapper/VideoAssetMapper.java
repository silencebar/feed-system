package com.example.feedsystem.video.mapper;

import com.example.feedsystem.video.model.VideoAssetDO;
import java.time.OffsetDateTime;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface VideoAssetMapper {
    int insert(VideoAssetDO asset);
    VideoAssetDO selectById(Long videoId);
    int updateValidation(@Param("videoId") Long videoId, @Param("accountId") Long accountId,
                         @Param("fileHash") String fileHash, @Param("mediaStatus") String mediaStatus,
                         @Param("updatedTime") OffsetDateTime updatedTime);
    int updateUploading(@Param("videoId") Long videoId, @Param("accountId") Long accountId,
                        @Param("updatedTime") OffsetDateTime updatedTime);
    int updateCompleted(@Param("videoId") Long videoId, @Param("accountId") Long accountId,
                        @Param("videoUrl") String videoUrl, @Param("updatedTime") OffsetDateTime updatedTime);
    int updateFailed(@Param("videoId") Long videoId, @Param("accountId") Long accountId,
                     @Param("updatedTime") OffsetDateTime updatedTime);
}
