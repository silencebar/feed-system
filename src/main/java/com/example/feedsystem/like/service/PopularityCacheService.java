package com.example.feedsystem.like.service;

import com.example.feedsystem.common.config.FeatureProperties;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PopularityCacheService {

    private static final DateTimeFormatter WINDOW_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmm");
    private static final Duration HOT_WINDOW_TTL = Duration.ofHours(2);

    private final StringRedisTemplate redisTemplate;
    private final FeatureProperties featureProperties;

    public void changeVideoPopularity(Long videoId, int change) {
        if (!featureProperties.isRedisCacheEnabled()) {
            log.info("Redis cache disabled, skip popularity cache update");
            return;
        }
        String key = "v1:hot:video:1m:" + LocalDateTime.now().format(WINDOW_FORMATTER);
        try {
            redisTemplate.opsForZSet().incrementScore(key, videoId.toString(), change);
            redisTemplate.expire(key, HOT_WINDOW_TTL);
        } catch (RuntimeException ex) {
            log.warn("Failed to update video popularity cache for video {}", videoId, ex);
        }
    }
}
