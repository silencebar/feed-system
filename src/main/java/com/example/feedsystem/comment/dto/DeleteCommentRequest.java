package com.example.feedsystem.comment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class DeleteCommentRequest {
    @NotNull
    @Positive
    @JsonProperty("comment_id")
    private Long commentId;
}
