package com.example.feedsystem.feed.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LikesCountFeedResponse {
    @JsonProperty("video_list")
    private List<FeedVideoItem> videoList;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("next_likes_count_before")
    private Long nextLikesCountBefore;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("next_id_before")
    private Long nextIdBefore;
    @JsonProperty("has_more")
    private boolean hasMore;
}
