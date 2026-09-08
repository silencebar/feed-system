package com.example.feedsystem.creatorassistant.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreatorSuggestRequest {
    @NotBlank
    @Size(max = 100)
    private String topic;

    @Size(max = 120)
    @JsonProperty("original_title")
    private String originalTitle = "";

    @Size(max = 1000)
    @JsonProperty("original_description")
    private String originalDescription = "";

    @Size(max = 30)
    private String style = "专业简洁";

    @Size(max = 50)
    @JsonProperty("target_audience")
    private String targetAudience = "普通用户";
}
