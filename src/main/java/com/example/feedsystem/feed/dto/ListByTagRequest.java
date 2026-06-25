package com.example.feedsystem.feed.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ListByTagRequest {
    @NotBlank
    @JsonProperty("tag_name")
    private String tagName;
    private Integer limit;
}
