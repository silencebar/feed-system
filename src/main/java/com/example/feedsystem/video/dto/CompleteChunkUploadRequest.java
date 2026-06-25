package com.example.feedsystem.video.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CompleteChunkUploadRequest {

    @NotBlank
    @JsonProperty("upload_id")
    private String uploadId;
}
