package com.example.feedsystem.video.service;

import com.example.feedsystem.common.config.FeatureProperties;
import com.example.feedsystem.video.dto.VideoResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoCacheService {

    private static final Duration DETAIL_TTL = Duration.ofMinutes(5);
    private static final Duration LOCK_TTL = Duration.ofSeconds(5);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final FeatureProperties featureProperties;

    public VideoResponse getDetail(Long videoId) {
        if (!featureProperties.isRedisCacheEnabled()) {
            log.info("Redis cache disabled, skip video detail cache read");
            return null;
        }
        String value = redisTemplate.opsForValue().get(detailKey(videoId));
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.readValue(value, VideoResponse.class);
        } catch (JsonProcessingException ex) {
            redisTemplate.delete(detailKey(videoId));
            return null;
        }
    }

    public void saveDetail(VideoResponse video) {
        if (!featureProperties.isRedisCacheEnabled()) {
            log.info("Redis cache disabled, skip video detail cache save");
            return;
        }
        try {
            redisTemplate.opsForValue().set(detailKey(video.getId()), objectMapper.writeValueAsString(video), DETAIL_TTL);
        } catch (JsonProcessingException ignored) {
        }
    }

    public Boolean tryLockDetail(Long videoId) {
        if (!featureProperties.isRedisCacheEnabled()) {
            log.info("Redis cache disabled, skip video detail cache lock");
            return false;
        }
        return redisTemplate.opsForValue().setIfAbsent(lockKey(videoId), "1", LOCK_TTL);
    }

    public void unlockDetail(Long videoId) {
        if (!featureProperties.isRedisCacheEnabled()) return;
        redisTemplate.delete(lockKey(videoId));
    }

    public void evictDetail(Long videoId) {
        if (!featureProperties.isRedisCacheEnabled()) {
            log.info("Redis cache disabled, skip video detail cache eviction");
            return;
        }
        redisTemplate.delete(detailKey(videoId));
        redisTemplate.delete("v1:video:entity:" + videoId);
    }

    private String detailKey(Long videoId) {
        return "v1:video:detail:id=" + videoId;
    }

    private String lockKey(Long videoId) {
        return "lock:" + detailKey(videoId);
    }
}
