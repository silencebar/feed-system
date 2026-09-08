package com.example.feedsystem.video.service;

import com.example.feedsystem.video.dto.VideoProbeResult;
import com.example.feedsystem.video.mapper.VideoMediaMetadataMapper;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest(properties = {
        "jwt.secret=test-secret-test-secret-test-secret-32",
        "spring.datasource.url=jdbc:h2:mem:metadata_tests;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "storage.minio.initialize=false", "spring.rabbitmq.listener.simple.auto-startup=false"
})
class VideoMediaMetadataServiceTests {
    @Autowired VideoMediaMetadataService service;
    @Autowired VideoMediaMetadataMapper mapper;
    @Autowired JdbcTemplate jdbc;
    @MockitoBean VideoAssetService assets;

    @BeforeEach void schema() {
        jdbc.execute("DROP TABLE IF EXISTS video_media_metadata");
        jdbc.execute("""
            CREATE TABLE video_media_metadata (
              video_id BIGINT PRIMARY KEY, duration_seconds DECIMAL(12,3) NOT NULL,
              width INT NOT NULL, height INT NOT NULL, video_codec VARCHAR(64) NOT NULL,
              has_audio BOOLEAN NOT NULL, audio_codec VARCHAR(64),
              probe_json JSON NOT NULL, probed_at TIMESTAMP(3) NOT NULL)
            """);
    }

    VideoProbeResult result(boolean audio) {
        return new VideoProbeResult(new BigDecimal("1.500"), 320, 240, "h264",
                audio, audio ? "aac" : null, "{\"streams\":[]}");
    }

    @Test void upsertKeepsOneRowAndMapsNullableAudio() {
        service.saveReady(1L, 2L, "hash", result(true));
        assertEquals("aac", mapper.selectByVideoId(1L).getAudioCodec());
        service.saveReady(1L, 2L, "hash", result(false));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM video_media_metadata", Integer.class));
        var saved = mapper.selectByVideoId(1L);
        assertEquals(new BigDecimal("1.500"), saved.getDurationSeconds());
        assertEquals(320, saved.getWidth());
        assertEquals(240, saved.getHeight());
        assertEquals("h264", saved.getVideoCodec());
        assertFalse(saved.getHasAudio());
        assertNull(saved.getAudioCodec());
        assertNotNull(saved.getProbeJson());
        assertNotNull(saved.getProbedAt());
        verify(assets, times(2)).recordValidation(1L, 2L, "hash", "READY");
    }

    @Test void failedReadyUpdateRollsBackMetadataInsert() {
        doThrow(new IllegalStateException("update failed")).when(assets)
                .recordValidation(1L, 2L, "hash", "READY");
        assertThrows(IllegalStateException.class, () -> service.saveReady(1L, 2L, "hash", result(false)));
        assertNull(mapper.selectByVideoId(1L));
    }

    @Test void failedReadyUpdateRollsBackMetadataReplacement() {
        service.saveReady(1L, 2L, "hash", result(true));
        doThrow(new IllegalStateException("update failed")).when(assets)
                .recordValidation(1L, 2L, "hash", "READY");
        assertThrows(IllegalStateException.class, () -> service.saveReady(1L, 2L, "hash", result(false)));
        assertTrue(mapper.selectByVideoId(1L).getHasAudio());
        assertEquals("aac", mapper.selectByVideoId(1L).getAudioCodec());
    }

    @Test void deniedOwnershipDoesNotWriteMetadata() {
        doThrow(new IllegalStateException("forbidden")).when(assets).requireOwned(1L, 2L);
        assertThrows(IllegalStateException.class, () -> service.saveReady(1L, 2L, "hash", result(false)));
        assertNull(mapper.selectByVideoId(1L));
        verify(assets, never()).recordValidation(any(), any(), any(), any());
    }
}
