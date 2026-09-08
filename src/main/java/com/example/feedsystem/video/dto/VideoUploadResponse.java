package com.example.feedsystem.video.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class VideoUploadResponse {
    private Long videoId;
    private String videoUrl;
    private String uploadStatus;
}
