package com.example.feedsystem.social.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class FollowRequest {

    @NotNull
    @Positive
    @JsonProperty("vlogger_id")
    private Long vloggerId;
}
