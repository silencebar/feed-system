package com.example.feedsystem.video.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class InitChunkUploadResponse {

    @JsonProperty("upload_id")
    private String uploadId;

    private Long videoId;

    @JsonProperty("uploaded_chunks")
    private List<Integer> uploadedChunks;
}
