package com.example.feedsystem.account.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class FindAccountByIdRequest {
    @NotNull
    @Positive
    private Long id;
}
