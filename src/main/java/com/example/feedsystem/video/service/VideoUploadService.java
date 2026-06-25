package com.example.feedsystem.video.service;

import com.example.feedsystem.common.BusinessException;
import com.example.feedsystem.storage.StorageService;
import com.example.feedsystem.video.dto.UploadResponse;
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
public class VideoUploadService {

    private static final long MAX_VIDEO_SIZE = 200L * 1024 * 1024;
    private static final long MAX_COVER_SIZE = 10L * 1024 * 1024;
    private static final Set<String> COVER_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".webp");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;

    private final SecureRandom secureRandom = new SecureRandom();
    private final StorageService storageService;

    public VideoUploadService(StorageService storageService) {
        this.storageService = storageService;
    }

    public UploadResponse uploadVideo(Long accountId, MultipartFile file) {
        String objectKey = save(file, accountId, "videos", ".mp4", MAX_VIDEO_SIZE);
        return UploadResponse.video(storageService.getUrl(objectKey));
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
