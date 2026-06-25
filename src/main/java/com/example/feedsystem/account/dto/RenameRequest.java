package com.example.feedsystem.account.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RenameRequest {

    @NotBlank
    private String username;
}
