package com.example.feedsystem.social.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SocialCountsResponse {

    @JsonProperty("follower_count")
    private long followerCount;

    @JsonProperty("vlogger_count")
    private long vloggerCount;
}
