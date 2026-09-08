package com.example.feedsystem.video.model;

import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
public class VideoMediaMetadataDO {
    private Long videoId;
    private BigDecimal durationSeconds;
    private Integer width;
    private Integer height;
    private String videoCodec;
    private Boolean hasAudio;
    private String audioCodec;
    private String probeJson;
    private OffsetDateTime probedAt;
}
