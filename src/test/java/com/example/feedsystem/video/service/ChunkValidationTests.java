package com.example.feedsystem.video.service;

import com.example.feedsystem.common.BusinessException;
import com.example.feedsystem.storage.StorageService;
import com.example.feedsystem.video.dto.ChunkUploadSession;
import com.example.feedsystem.video.dto.VideoUploadResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ChunkValidationTests {
    @TempDir Path root;
    StorageService storage = mock(StorageService.class);
    VideoAssetService assets = mock(VideoAssetService.class);
    FfprobeService probe = mock(FfprobeService.class);
    VideoMediaMetadataService metadata = mock(VideoMediaMetadataService.class);

    @SuppressWarnings("unchecked")
    ChunkUploadService service(String hash) throws Exception {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> values = mock(ValueOperations.class);
        SetOperations<String, String> sets = mock(SetOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(redis.opsForSet()).thenReturn(sets);
        when(sets.size(anyString())).thenReturn(2L);
        ChunkUploadSession session = new ChunkUploadSession();
        session.setUploadId("session");
        session.setAccountId(2L);
        session.setVideoId(1L);
        session.setObjectKey("videos/2/fixed.mp4");
        session.setFilename("silent.mp4");
        session.setTotalChunks(2);
        session.setFileSize(3L);
        session.setFileHash(hash);
        ObjectMapper mapper = new ObjectMapper();
        when(values.get("v1:chunk_upload:session")).thenReturn(mapper.writeValueAsString(session));
        Files.createDirectories(root.resolve("tmp/session"));
        Files.writeString(root.resolve("tmp/session/0"), "ab");
        Files.writeString(root.resolve("tmp/session/1"), "c");
        ChunkUploadService service = new ChunkUploadService(redis, mapper, mock(VideoUploadService.class),
                assets, storage, new VideoValidationService(probe, assets, metadata));
        ReflectionTestUtils.setField(service, "uploadRoot", root.toString());
        return service;
    }

    @Test void badHashNeverReachesProbeOrMinioAndDiscardsBadChunks() throws Exception {
        var service = service("0".repeat(32));
        assertThrows(BusinessException.class, () -> service.complete(2L, "session"));
        verifyNoInteractions(probe, storage);
        verify(assets).markFailedQuietly(1L, 2L);
        assertFalse(Files.exists(root.resolve("tmp/session")));
    }
    @Test void invalidMediaNeverReachesMinio() throws Exception {
        var service = service("900150983cd24fb0d6963f7d28e17f72");
        doThrow(new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "invalid")).when(probe).validate(any());
        assertThrows(BusinessException.class, () -> service.complete(2L, "session"));
        verifyNoInteractions(storage);
        verify(assets).markFailedQuietly(1L, 2L);
    }
    @Test void probeSucceedsBeforeMinioAndCompletion() throws Exception {
        var service = service("900150983cd24fb0d6963f7d28e17f72");
        when(storage.getUrl(anyString())).thenReturn("https://example/video");
        when(assets.markCompleted(1L, 2L, "https://example/video"))
                .thenReturn(new VideoUploadResponse(1L, "https://example/video", "COMPLETED"));
        assertEquals("COMPLETED", service.complete(2L, "session").getUploadStatus());
        var order = inOrder(probe, storage, assets, metadata);
        order.verify(probe).validate(any());
        order.verify(metadata).saveReady(1L, 2L, "900150983cd24fb0d6963f7d28e17f72", null);
        order.verify(storage).upload(any(), eq("videos/2/fixed.mp4"));
        order.verify(storage).getUrl(anyString());
        order.verify(assets).markCompleted(1L, 2L, "https://example/video");
        assertFalse(Files.exists(root.resolve("tmp/session")));
    }

    @Test void metadataSaveFailurePreventsMinioUpload() throws Exception {
        var service = service("900150983cd24fb0d6963f7d28e17f72");
        doThrow(new IllegalStateException("database unavailable")).when(metadata)
                .saveReady(eq(1L), eq(2L), anyString(), isNull());
        assertThrows(IllegalStateException.class, () -> service.complete(2L, "session"));
        verifyNoInteractions(storage);
        verify(assets).markFailedQuietly(1L, 2L);
    }
}
