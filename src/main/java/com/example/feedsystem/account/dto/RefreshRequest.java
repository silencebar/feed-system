package com.example.feedsystem.account.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RefreshRequest {

    @NotBlank
    @JsonProperty("refresh_token")
    private String refreshToken;
}
