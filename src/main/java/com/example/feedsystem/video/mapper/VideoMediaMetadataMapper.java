package com.example.feedsystem.video.mapper;

import com.example.feedsystem.video.model.VideoMediaMetadataDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface VideoMediaMetadataMapper {
    int upsert(VideoMediaMetadataDO metadata);
    VideoMediaMetadataDO selectByVideoId(@Param("videoId") Long videoId);
}
