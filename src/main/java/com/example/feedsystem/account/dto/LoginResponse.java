package com.example.feedsystem.account.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginResponse {

    private String token;

    @JsonProperty("refresh_token")
    private String refreshToken;

    @JsonProperty("account_id")
    private Long accountId;

    private String username;
}
