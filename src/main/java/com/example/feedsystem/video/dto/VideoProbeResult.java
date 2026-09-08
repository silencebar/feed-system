package com.example.feedsystem.video.dto;

import java.math.BigDecimal;

/** Technical metadata only; hasAudio does not mean audible speech exists. */
public record VideoProbeResult(BigDecimal durationSeconds, int width, int height,
        String videoCodec, boolean hasAudio, String audioCodec, String rawJson) {}
