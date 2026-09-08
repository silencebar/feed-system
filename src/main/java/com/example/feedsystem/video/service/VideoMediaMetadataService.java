package com.example.feedsystem.video.service;

import com.example.feedsystem.video.dto.VideoProbeResult;
import com.example.feedsystem.video.mapper.VideoMediaMetadataMapper;
import com.example.feedsystem.video.model.VideoMediaMetadataDO;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class VideoMediaMetadataService {
    private final VideoMediaMetadataMapper mapper;
    private final VideoAssetService assets;

    /** Probe execution happens before this short database transaction. */
    @Transactional
    public void saveReady(Long videoId, Long accountId, String actualHash, VideoProbeResult result) {
        assets.requireOwned(videoId, accountId);
        VideoMediaMetadataDO metadata = new VideoMediaMetadataDO();
        metadata.setVideoId(videoId);
        metadata.setDurationSeconds(result.durationSeconds());
        metadata.setWidth(result.width());
        metadata.setHeight(result.height());
        metadata.setVideoCodec(result.videoCodec());
        metadata.setHasAudio(result.hasAudio());
        metadata.setAudioCodec(result.audioCodec());
        metadata.setProbeJson(result.rawJson());
        metadata.setProbedAt(OffsetDateTime.now());
        mapper.upsert(metadata);
        assets.recordValidation(videoId, accountId, actualHash, "READY");
    }
}
