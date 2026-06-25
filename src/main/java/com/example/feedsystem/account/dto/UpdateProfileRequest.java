package com.example.feedsystem.account.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequest {

    @Size(max = 512)
    @JsonProperty("avatar_url")
    private String avatarUrl;

    @Size(max = 255)
    private String bio;
}
