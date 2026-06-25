package com.example.feedsystem.account.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class FindAccountByUsernameRequest {
    @NotBlank
    private String username;
}
