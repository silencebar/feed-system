package com.example.feedsystem.video.service;

import com.example.feedsystem.common.config.FeatureProperties;
import com.example.feedsystem.common.BusinessException;
import com.example.feedsystem.feed.service.TimelineService;
import com.example.feedsystem.storage.StorageObjectKeyResolver;
import com.example.feedsystem.storage.StorageService;
import com.example.feedsystem.video.dto.PublishVideoRequest;
import com.example.feedsystem.video.dto.VideoResponse;
import com.example.feedsystem.video.mapper.VideoMapper;
import com.example.feedsystem.video.model.OutboxMsgDO;
import com.example.feedsystem.video.model.VideoDO;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoService {

    private static final int LIST_LIMIT = 200;

    private final VideoMapper videoMapper;
    private final VideoCacheService videoCacheService;
    private final TagService tagService;
    private final TimelineService timelineService;
    private final StorageService storageService;
    private final StorageObjectKeyResolver storageObjectKeyResolver;
    private final FeatureProperties featureProperties;

    @Transactional
    public VideoResponse publish(Long authorId, String username, PublishVideoRequest request) {
        VideoDO video = new VideoDO();
        video.setAuthorId(authorId);
        video.setUsername(username);
        video.setTitle(request.getTitle());
        video.setDescription(request.getDescription());
        video.setPlayUrl(storageObjectKeyResolver.toObjectKey(request.getPlayUrl()));
        video.setCoverUrl(storageObjectKeyResolver.toObjectKey(request.getCoverUrl()));
        video.setCreateTime(OffsetDateTime.now());
        video.setLikesCount(0L);
        video.setPopularity(0L);
        videoMapper.insertVideo(video);

        OutboxMsgDO outboxMsg = new OutboxMsgDO();
        outboxMsg.setVideoId(video.getId());
        outboxMsg.setEventType("video_published");
        outboxMsg.setCreateTime(OffsetDateTime.now());
        outboxMsg.setStatus("pending");
        videoMapper.insertOutbox(outboxMsg);

        for (String tagName : tagService.extractTags(request.getTitle(), request.getDescription())) {
            Long tagId = tagService.selectOrInsert(tagName);
            videoMapper.insertVideoTag(video.getId(), tagId);
        }
        timelineService.addVideo(video);
        return VideoResponse.from(video, storageService);
    }

    public List<VideoResponse> listByAuthorId(Long authorId) {
        return videoMapper.selectByAuthorId(authorId, LIST_LIMIT).stream()
                .map(video -> VideoResponse.from(video, storageService))
                .toList();
    }

    public VideoResponse getDetail(Long id) {
        if (!featureProperties.isRedisCacheEnabled()) {
            log.info("Redis cache disabled, fallback to MySQL: video.getDetail");
            return requireVideo(id);
        }
        log.info("Redis cache enabled: video.getDetail");
        VideoResponse cached = videoCacheService.getDetail(id);
        if (cached != null) {
            return cached;
        }

        Boolean locked = videoCacheService.tryLockDetail(id);
        if (Boolean.TRUE.equals(locked)) {
            try {
                VideoResponse response = requireVideo(id);
                videoCacheService.saveDetail(response);
                return response;
            } finally {
                videoCacheService.unlockDetail(id);
            }
        }

        sleepBriefly();
        cached = videoCacheService.getDetail(id);
        if (cached != null) {
            return cached;
        }
        return requireVideo(id);
    }

    @Transactional
    public void delete(Long accountId, Long id) {
        VideoDO video = videoMapper.selectById(id);
        if (video == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "video not found");
        }
        if (!accountId.equals(video.getAuthorId())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "can not delete others video");
        }
        videoMapper.deleteById(id);
        storageService.delete(storageObjectKeyResolver.toObjectKey(video.getPlayUrl()));
        storageService.delete(storageObjectKeyResolver.toObjectKey(video.getCoverUrl()));
        videoCacheService.evictDetail(id);
    }

    private VideoResponse requireVideo(Long id) {
        VideoDO video = videoMapper.selectById(id);
        if (video == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "video not found");
        }
        return VideoResponse.from(video, storageService);
    }

    private void sleepBriefly() {
        try {
            Thread.sleep(80);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
