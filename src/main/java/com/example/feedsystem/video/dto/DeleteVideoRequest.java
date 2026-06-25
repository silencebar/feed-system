package com.example.feedsystem.video.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class DeleteVideoRequest {

    @NotNull
    @Positive
    private Long id;
}
