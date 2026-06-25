package com.example.feedsystem.feed.dto;

import com.example.feedsystem.storage.StorageService;
import com.example.feedsystem.video.model.VideoDO;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FeedVideoItem {
    private Long id;
    private FeedAuthor author;
    private String title;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String description;
    @JsonProperty("play_url")
    private String playUrl;
    @JsonProperty("cover_url")
    private String coverUrl;
    @JsonProperty("create_time")
    private long createTime;
    @JsonProperty("likes_count")
    private long likesCount;
    @JsonProperty("is_liked")
    private boolean liked;

    public static FeedVideoItem from(VideoDO video) {
        return from(video, null);
    }

    public static FeedVideoItem from(VideoDO video, StorageService storageService) {
        return new FeedVideoItem(video.getId(), new FeedAuthor(video.getAuthorId(), video.getUsername()),
                video.getTitle(), video.getDescription(),
                storageService == null ? video.getPlayUrl() : storageService.getUrl(video.getPlayUrl()),
                storageService == null ? video.getCoverUrl() : storageService.getUrl(video.getCoverUrl()),
                video.getCreateTime().toEpochSecond(), video.getLikesCount(), false);
    }
}
