package com.example.feedsystem.video.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class InitChunkUploadRequest {

    @NotBlank
    private String filename;

    @NotNull
    @Positive
    @JsonProperty("file_size")
    private Long fileSize;

    @NotNull
    @Positive
    @JsonProperty("chunk_size")
    private Long chunkSize;

    @NotNull
    @Positive
    @JsonProperty("total_chunks")
    private Integer totalChunks;

    @NotBlank
    @JsonProperty("file_hash")
    private String fileHash;
}
