package com.example.feedsystem.video.dto;

import com.example.feedsystem.video.model.VideoDO;
import com.example.feedsystem.storage.StorageService;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VideoResponse {

    private Long id;

    @JsonProperty("author_id")
    private Long authorId;

    private String username;
    private String title;
    private String description;

    @JsonProperty("play_url")
    private String playUrl;

    @JsonProperty("cover_url")
    private String coverUrl;

    @JsonProperty("create_time")
    private OffsetDateTime createTime;

    @JsonProperty("likes_count")
    private Long likesCount;

    private Long popularity;

    public static VideoResponse from(VideoDO video) {
        return from(video, null);
    }

    public static VideoResponse from(VideoDO video, StorageService storageService) {
        return new VideoResponse(
                video.getId(),
                video.getAuthorId(),
                video.getUsername(),
                video.getTitle(),
                video.getDescription(),
                storageService == null ? video.getPlayUrl() : storageService.getUrl(video.getPlayUrl()),
                storageService == null ? video.getCoverUrl() : storageService.getUrl(video.getCoverUrl()),
                video.getCreateTime(),
                video.getLikesCount(),
                video.getPopularity()
        );
    }
}
