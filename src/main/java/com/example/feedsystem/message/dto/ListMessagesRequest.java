package com.example.feedsystem.message.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class ListMessagesRequest {
    @NotNull
    @Positive
    @JsonProperty("peer_id")
    private Long peerId;
}
