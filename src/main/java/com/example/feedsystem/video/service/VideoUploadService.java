package com.example.feedsystem.video.service;

import com.example.feedsystem.common.BusinessException;
import com.example.feedsystem.storage.StorageService;
import com.example.feedsystem.storage.PathMultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.extern.slf4j.Slf4j;
import com.example.feedsystem.video.dto.UploadResponse;
import com.example.feedsystem.video.dto.VideoUploadResponse;
import com.example.feedsystem.video.model.VideoAssetDO;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@Slf4j
public class VideoUploadService {

    private static final long MAX_VIDEO_SIZE = 200L * 1024 * 1024;
    private static final long MAX_COVER_SIZE = 10L * 1024 * 1024;
    private static final Set<String> COVER_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".webp");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;

    private final SecureRandom secureRandom = new SecureRandom();
    private final StorageService storageService;
    private final VideoAssetService videoAssetService;
    private final VideoValidationService videoValidationService;

    public VideoUploadService(StorageService storageService, VideoAssetService videoAssetService,
                              VideoValidationService videoValidationService) {
        this.storageService = storageService;
        this.videoAssetService = videoAssetService;
        this.videoValidationService = videoValidationService;
    }

    public VideoUploadResponse uploadVideo(Long accountId, MultipartFile file) {
        validateFile(file, MAX_VIDEO_SIZE);
        if (!extension(file).equals(".mp4")) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "invalid file format");
        }
        String objectKey = objectKey(accountId, "videos", ".mp4");
        String originalFileName = file.getOriginalFilename() == null ? "video.mp4" : file.getOriginalFilename();
        VideoAssetDO asset = videoAssetService.create(accountId, null, objectKey, originalFileName,
                file.getSize(), null);
        Path temp = null;
        try {
            temp = Files.createTempFile("video-validation-", ".mp4");
            file.transferTo(temp);
            videoValidationService.validate(asset.getVideoId(), accountId, temp, file.getSize(), null);
            storageService.upload(new PathMultipartFile(temp, "file", originalFileName, "video/mp4"), objectKey);
            return videoAssetService.markCompleted(asset.getVideoId(), accountId, storageService.getUrl(objectKey));
        } catch (IOException ex) {
            videoAssetService.markFailedQuietly(asset.getVideoId(), accountId);
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "failed to stage video for validation");
        } catch (RuntimeException ex) {
            videoAssetService.markFailedQuietly(asset.getVideoId(), accountId);
            throw ex;
        } finally {
            if (temp != null) {
                try {
                    Files.deleteIfExists(temp);
                } catch (IOException ex) {
                    log.warn("Failed to remove validation temp file: {}", temp, ex);
                }
            }
        }
    }

    public UploadResponse uploadCover(Long accountId, MultipartFile file) {
        String extension = extension(file);
        if (!COVER_EXTENSIONS.contains(extension)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "invalid cover format");
        }
        String objectKey = save(file, accountId, "covers", extension, MAX_COVER_SIZE);
        return UploadResponse.cover(storageService.getUrl(objectKey));
    }

    public String videoObjectKey(Long accountId) {
        return objectKey(accountId, "videos", ".mp4");
    }

    public String randomFilename(String extension) {
        byte[] bytes = new byte[16];
        secureRandom.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes) + extension;
    }

    private String save(MultipartFile file, Long accountId, String type, String extension, long maxSize) {
        validateFile(file, maxSize);
        if (!extension(file).equals(extension)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "invalid file format");
        }

        String objectKey = objectKey(accountId, type, extension);
        storageService.upload(file, objectKey);
        return objectKey;
    }

    private String objectKey(Long accountId, String type, String extension) {
        String date = LocalDate.now().format(DATE_FORMATTER);
        return type + "/" + accountId + "/" + date + "/" + randomFilename(extension);
    }

    private void validateFile(MultipartFile file, long maxSize) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "file is required");
        }
        if (file.getSize() > maxSize) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "file too large");
        }
    }

    private String extension(MultipartFile file) {
        String name = file == null ? "" : file.getOriginalFilename();
        int dot = name == null ? -1 : name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot).toLowerCase(Locale.ROOT);
    }
}
