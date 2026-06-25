package com.example.feedsystem.like.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class IsLikedResponse {

    @JsonProperty("is_liked")
    private boolean liked;
}
