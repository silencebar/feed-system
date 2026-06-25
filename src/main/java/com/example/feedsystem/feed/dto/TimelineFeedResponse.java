package com.example.feedsystem.feed.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TimelineFeedResponse {
    @JsonProperty("video_list")
    private List<FeedVideoItem> videoList;
    @JsonProperty("next_time")
    private long nextTime;
    @JsonProperty("has_more")
    private boolean hasMore;
}
