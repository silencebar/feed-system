package com.example.feedsystem.feed.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import lombok.Data;

@Data
public class ListByPopularityRequest {
    private Integer limit;
    @JsonProperty("as_of")
    private Long asOf;
    private Long offset;
    @JsonProperty("latest_popularity")
    private Long latestPopularity;
    @JsonProperty("latest_before")
    private OffsetDateTime latestBefore;
    @JsonProperty("latest_id_before")
    private Long latestIdBefore;
}
