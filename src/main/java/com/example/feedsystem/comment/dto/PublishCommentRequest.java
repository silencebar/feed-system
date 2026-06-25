package com.example.feedsystem.comment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class PublishCommentRequest {
    @NotNull
    @Positive
    @JsonProperty("video_id")
    private Long videoId;

    @NotBlank
    private String content;
}
