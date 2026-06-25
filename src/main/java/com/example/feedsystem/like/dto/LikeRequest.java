package com.example.feedsystem.like.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class LikeRequest {

    @NotNull
    @Positive
    @JsonProperty("video_id")
    private Long videoId;
}
