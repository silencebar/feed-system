package com.example.feedsystem.social.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

@Data
public class GetAllVloggersRequest {

    @PositiveOrZero
    @JsonProperty("follower_id")
    private Long followerId;
}
