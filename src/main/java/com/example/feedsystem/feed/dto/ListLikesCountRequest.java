package com.example.feedsystem.feed.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ListLikesCountRequest {
    private Integer limit;
    @JsonProperty("likes_count_before")
    private Long likesCountBefore;
    @JsonProperty("id_before")
    private Long idBefore;
}
