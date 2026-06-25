package com.example.feedsystem.feed.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ListByFollowingRequest {
    private Integer limit;
    @JsonProperty("latest_time")
    private Long latestTime;
}
