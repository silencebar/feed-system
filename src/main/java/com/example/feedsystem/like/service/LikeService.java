package com.example.feedsystem.like.service;

import com.example.feedsystem.common.BusinessException;
import com.example.feedsystem.like.mapper.LikeMapper;
import com.example.feedsystem.storage.StorageService;
import com.example.feedsystem.video.dto.VideoResponse;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LikeService {

    private static final int LIST_LIMIT = 200;

    private final LikeMapper likeMapper;
    private final LikeRateLimiter likeRateLimiter;
    private final LikeEventPublisher likeEventPublisher;
    private final StorageService storageService;

    @Transactional
    public void like(Long accountId, Long videoId) {
        checkWriteRateLimit(accountId);
        requireVideo(videoId);
        if (likeMapper.existsByVideoIdAndAccountId(videoId, accountId)) {
            throw new BusinessException(HttpStatus.CONFLICT, "user has liked this video");
        }

        try {
            likeMapper.insertLike(videoId, accountId, OffsetDateTime.now());
        } catch (DuplicateKeyException ex) {
            throw new BusinessException(HttpStatus.CONFLICT, "user has liked this video");
        }
        likeMapper.changeVideoStats(videoId, 1, 1);
        likeEventPublisher.publishLike(accountId, videoId);
    }

    @Transactional
    public void unlike(Long accountId, Long videoId) {
        checkWriteRateLimit(accountId);
        requireVideo(videoId);
        if (!likeMapper.existsByVideoIdAndAccountId(videoId, accountId)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "user has not liked this video");
        }

        int deleted = likeMapper.deleteByVideoIdAndAccountId(videoId, accountId);
        if (deleted == 0) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "user has not liked this video");
        }
        likeMapper.changeVideoStats(videoId, -1, -1);
        likeEventPublisher.publishUnlike(accountId, videoId);
    }

    public boolean isLiked(Long accountId, Long videoId) {
        requireVideo(videoId);
        return likeMapper.existsByVideoIdAndAccountId(videoId, accountId);
    }

    public List<VideoResponse> listMyLikedVideos(Long accountId) {
        return likeMapper.selectLikedVideosByAccountId(accountId, LIST_LIMIT).stream()
                .map(video -> VideoResponse.from(video, storageService))
                .toList();
    }

    private void requireVideo(Long videoId) {
        if (!likeMapper.existsVideoById(videoId)) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "video not found");
        }
    }

    private void checkWriteRateLimit(Long accountId) {
        if (!likeRateLimiter.allowLikeWrite(accountId)) {
            throw new BusinessException(HttpStatus.TOO_MANY_REQUESTS, "too many requests");
        }
    }
}
