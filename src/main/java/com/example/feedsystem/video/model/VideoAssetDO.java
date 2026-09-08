package com.example.feedsystem.video.model;

import java.time.OffsetDateTime;
import lombok.Data;

@Data
public class VideoAssetDO {

    private Long videoId;
    private Long accountId;
    private String uploadId;
    private String objectKey;
    private String videoUrl;
    private String originalFileName;
    private Long fileSize;
    private String fileHash;
    private String uploadStatus;
    private String mediaStatus;
    private OffsetDateTime createdTime;
    private OffsetDateTime updatedTime;
}
