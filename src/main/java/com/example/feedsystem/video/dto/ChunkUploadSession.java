package com.example.feedsystem.video.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class ChunkUploadSession {

    @JsonProperty("upload_id")
    private String uploadId;

    @JsonProperty("account_id")
    private Long accountId;

    private String filename;

    @JsonProperty("file_size")
    private Long fileSize;

    @JsonProperty("chunk_size")
    private Long chunkSize;

    @JsonProperty("total_chunks")
    private Integer totalChunks;

    @JsonProperty("file_hash")
    private String fileHash;

    @JsonProperty("uploaded_chunks")
    private List<Integer> uploadedChunks = new ArrayList<>();
}
