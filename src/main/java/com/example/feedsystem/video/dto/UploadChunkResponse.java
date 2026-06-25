package com.example.feedsystem.video.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UploadChunkResponse {

    @JsonProperty("chunk_index")
    private Integer chunkIndex;
}
