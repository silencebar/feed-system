package com.example.feedsystem.creatorassistant.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Data;

@Data
public class CreatorSuggestResponse {
    @JsonProperty("title_candidates")
    private List<String> titleCandidates;

    @JsonProperty("recommended_title")
    private String recommendedTitle;

    private String description;
    private List<String> tags;

    @JsonProperty("risk_level")
    private String riskLevel;

    private List<String> warnings;
}
