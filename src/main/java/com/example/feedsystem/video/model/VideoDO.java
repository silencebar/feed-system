package com.example.feedsystem.video.model;

import java.time.OffsetDateTime;
import lombok.Data;

@Data
public class VideoDO {

    private Long id;
    private Long assetId;
    private Long authorId;
    private String username;
    private String title;
    private String description;
    private String playUrl;
    private String coverUrl;
    private OffsetDateTime createTime;
    private Long likesCount;
    private Long popularity;
}
