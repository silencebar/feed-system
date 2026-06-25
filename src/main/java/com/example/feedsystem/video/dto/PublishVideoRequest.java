package com.example.feedsystem.video.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PublishVideoRequest {

    @NotBlank
    private String title;

    private String description;

    @NotBlank
    @JsonProperty("play_url")
    private String playUrl;

    @NotBlank
    @JsonProperty("cover_url")
    private String coverUrl;
}
