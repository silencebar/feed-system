package com.example.feedsystem.account.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RefreshResponse {

    private String token;

    @JsonProperty("account_id")
    private Long accountId;

    private String username;
}
