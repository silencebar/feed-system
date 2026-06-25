package com.example.feedsystem.social.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

@Data
public class GetAllFollowersRequest {

    @PositiveOrZero
    @JsonProperty("vlogger_id")
    private Long vloggerId;
}
