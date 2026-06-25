package com.example.feedsystem.feed.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PopularityFeedResponse {
    @JsonProperty("video_list")
    private List<FeedVideoItem> videoList;
    @JsonProperty("as_of")
    private long asOf;
    @JsonProperty("next_offset")
    private long nextOffset;
    @JsonProperty("has_more")
    private boolean hasMore;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("next_latest_popularity")
    private Long nextLatestPopularity;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("next_latest_before")
    private OffsetDateTime nextLatestBefore;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("next_latest_id_before")
    private Long nextLatestIdBefore;
}
