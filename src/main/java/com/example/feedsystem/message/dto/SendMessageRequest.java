package com.example.feedsystem.message.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class SendMessageRequest {
    @NotNull
    @Positive
    @JsonProperty("to_id")
    private Long toId;
    @NotBlank
    private String content;
}
