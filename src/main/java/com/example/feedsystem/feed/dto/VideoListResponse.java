package com.example.feedsystem.feed.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class VideoListResponse {
    @JsonProperty("video_list")
    private List<FeedVideoItem> videoList;
}
