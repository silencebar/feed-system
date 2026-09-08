package com.example.feedsystem.video.service;

import com.example.feedsystem.common.BusinessException;
import com.example.feedsystem.video.dto.VideoProbeResult;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class VideoValidationService {
    public static class IntegrityException extends BusinessException {
        public IntegrityException(String message) { super(HttpStatus.BAD_REQUEST, message); }
    }
    private final FfprobeService ffprobe;
    private final VideoAssetService assets;
    private final VideoMediaMetadataService metadata;

    public void validate(Long videoId, Long accountId, Path path, long expectedSize, String expectedHash) throws IOException {
        String actualHash = verifyIntegrity(path, expectedSize, expectedHash);
        assets.recordValidation(videoId, accountId, actualHash, "PROCESSING");
        VideoProbeResult result;
        try {
            result = ffprobe.validate(path);
        } catch (BusinessException ex) {
            // Infrastructure failure is not proof that the media is invalid.
            assets.recordValidation(videoId, accountId, actualHash,
                    ex.getStatus() == HttpStatus.UNPROCESSABLE_ENTITY ? "FAILED" : "PENDING");
            throw ex;
        }
        metadata.saveReady(videoId, accountId, actualHash, result);
    }

    String verifyIntegrity(Path path, long expectedSize, String expectedHash) throws IOException {
        if (Files.size(path) != expectedSize || expectedSize <= 0 || expectedSize > 200L * 1024 * 1024) {
            throw new IntegrityException("merged file size mismatch; retry upload");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            try (var input = new DigestInputStream(Files.newInputStream(path), digest)) {
                input.transferTo(OutputStream.nullOutputStream());
            }
            String actual = HexFormat.of().formatHex(digest.digest());
            if (expectedHash != null && !actual.equalsIgnoreCase(expectedHash)) {
                throw new IntegrityException("merged file MD5 mismatch; retry upload");
            }
            return actual;
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("MD5 unavailable", ex);
        }
    }
}
