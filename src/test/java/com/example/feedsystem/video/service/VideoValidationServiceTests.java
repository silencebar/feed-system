package com.example.feedsystem.video.service;

import com.example.feedsystem.common.BusinessException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class VideoValidationServiceTests {
    @TempDir Path directory;
    private final FfprobeService probe = mock(FfprobeService.class);
    private final VideoAssetService assets = mock(VideoAssetService.class);
    private final VideoMediaMetadataService metadata = mock(VideoMediaMetadataService.class);
    private final VideoValidationService service = new VideoValidationService(probe, assets, metadata);
    private static final String HASH = "900150983cd24fb0d6963f7d28e17f72";

    private Path file() throws Exception {
        return Files.writeString(directory.resolve("merged.mp4"), "abc");
    }

    @Test void matchesUppercaseClientMd5AndRecordsReady() throws Exception {
        Path file = file();
        service.validate(1L, 2L, file, 3, HASH.toUpperCase());
        var order = inOrder(assets, probe, metadata);
        order.verify(assets).recordValidation(1L, 2L, HASH, "PROCESSING");
        order.verify(probe).validate(file);
        order.verify(metadata).saveReady(1L, 2L, HASH, null);
    }

    @Test void incorrectMd5StopsBeforeProbe() throws Exception {
        Path file = file();
        assertThrows(BusinessException.class, () -> service.validate(1L, 2L, file, 3, "0".repeat(32)));
        verifyNoInteractions(probe, assets);
    }

    @Test void incorrectSizeStopsBeforeProbe() throws Exception {
        Path file = file();
        assertThrows(BusinessException.class, () -> service.validate(1L, 2L, file, 4, HASH));
        verifyNoInteractions(probe, assets);
    }

    @Test void directUploadComputesHashWithoutClientComparison() throws Exception {
        assertEquals(HASH, service.verifyIntegrity(file(), 3, null));
    }

    @Test void invalidMediaIsFailedButProbeOutageIsPending() throws Exception {
        Path file = file();
        doThrow(new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "invalid")).when(probe).validate(file);
        assertThrows(BusinessException.class, () -> service.validate(1L, 2L, file, 3, HASH));
        verify(assets).recordValidation(1L, 2L, HASH, "FAILED");
        doThrow(new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "offline")).when(probe).validate(file);
        assertThrows(BusinessException.class, () -> service.validate(1L, 2L, file, 3, HASH));
        verify(assets).recordValidation(1L, 2L, HASH, "PENDING");
        verifyNoInteractions(metadata);
    }
}
