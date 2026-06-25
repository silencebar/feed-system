package com.example.feedsystem.video.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class ListByAuthorIdRequest {

    @NotNull
    @Positive
    @JsonProperty("author_id")
    private Long authorId;
}
