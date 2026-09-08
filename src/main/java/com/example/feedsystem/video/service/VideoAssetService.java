package com.example.feedsystem.video.service;

import com.example.feedsystem.common.BusinessException;
import com.example.feedsystem.video.dto.VideoUploadResponse;
import com.example.feedsystem.video.mapper.VideoAssetMapper;
import com.example.feedsystem.video.model.VideoAssetDO;
import com.example.feedsystem.video.model.VideoMediaStatus;
import com.example.feedsystem.video.model.VideoUploadStatus;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoAssetService {
    private final VideoAssetMapper videoAssetMapper;

    public VideoAssetDO create(Long accountId, String uploadId, String objectKey, String originalFileName,
                               Long fileSize, String fileHash) {
        VideoAssetDO asset = new VideoAssetDO();
        OffsetDateTime now = OffsetDateTime.now();
        asset.setAccountId(accountId);
        asset.setUploadId(uploadId);
        asset.setObjectKey(objectKey);
        asset.setOriginalFileName(originalFileName);
        asset.setFileSize(fileSize);
        asset.setFileHash(fileHash);
        asset.setUploadStatus(VideoUploadStatus.UPLOADING.name());
        asset.setMediaStatus(VideoMediaStatus.PENDING.name());
        asset.setCreatedTime(now);
        asset.setUpdatedTime(now);
        videoAssetMapper.insert(asset);
        return asset;
    }

    public VideoAssetDO requireOwned(Long videoId, Long accountId) {
        VideoAssetDO asset = videoAssetMapper.selectById(videoId);
        if (asset == null) throw new BusinessException(HttpStatus.NOT_FOUND, "video asset not found");
        if (!accountId.equals(asset.getAccountId())) throw new BusinessException(HttpStatus.FORBIDDEN, "video asset forbidden");
        return asset;
    }

    public VideoAssetDO requireCompleted(Long videoId, Long accountId) {
        VideoAssetDO asset = requireOwned(videoId, accountId);
        if (!VideoUploadStatus.COMPLETED.name().equals(asset.getUploadStatus())) {
            throw new BusinessException(HttpStatus.CONFLICT, "video upload not completed");
        }
        return asset;
    }

    public void markUploading(Long videoId, Long accountId) {
        requireUpdated(videoAssetMapper.updateUploading(videoId, accountId, OffsetDateTime.now()));
    }

    public void recordValidation(Long videoId, Long accountId, String fileHash, String mediaStatus) {
        requireUpdated(videoAssetMapper.updateValidation(videoId, accountId, fileHash, mediaStatus, OffsetDateTime.now()));
    }

    public VideoUploadResponse markCompleted(Long videoId, Long accountId, String videoUrl) {
        requireUpdated(videoAssetMapper.updateCompleted(videoId, accountId, videoUrl, OffsetDateTime.now()));
        return new VideoUploadResponse(videoId, videoUrl, VideoUploadStatus.COMPLETED.name());
    }

    public void markFailedQuietly(Long videoId, Long accountId) {
        if (videoId == null) return;
        try {
            videoAssetMapper.updateFailed(videoId, accountId, OffsetDateTime.now());
        } catch (RuntimeException ex) {
            log.warn("Failed to mark video asset as FAILED: videoId={}", videoId, ex);
        }
    }

    private void requireUpdated(int count) {
        if (count != 1) throw new BusinessException(HttpStatus.NOT_FOUND, "video asset not found");
    }
}
