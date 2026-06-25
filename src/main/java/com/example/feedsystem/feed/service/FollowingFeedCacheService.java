package com.example.feedsystem.feed.service;

import com.example.feedsystem.feed.dto.TimelineFeedResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FollowingFeedCacheService {
    private static final Duration CACHE_TTL = Duration.ofHours(24);
    private static final Duration LOCK_TTL = Duration.ofMillis(500);
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public TimelineFeedResponse get(int limit, long accountId, long before) {
        try {
            String value = redisTemplate.opsForValue().get(cacheKey(limit, accountId, before));
            return value == null ? null : objectMapper.readValue(value, TimelineFeedResponse.class);
        } catch (RuntimeException | JsonProcessingException ex) {
            return null;
        }
    }

    public void save(int limit, long accountId, long before, TimelineFeedResponse response) {
        try {
            redisTemplate.opsForValue().set(cacheKey(limit, accountId, before),
                    objectMapper.writeValueAsString(response), CACHE_TTL);
        } catch (RuntimeException | JsonProcessingException ignored) {
        }
    }

    public boolean tryLock(int limit, long accountId, long before) {
        try {
            return Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(
                    "lock:" + cacheKey(limit, accountId, before), "1", LOCK_TTL));
        } catch (RuntimeException ex) {
            return true;
        }
    }

    public void unlock(int limit, long accountId, long before) {
        try {
            redisTemplate.delete("lock:" + cacheKey(limit, accountId, before));
        } catch (RuntimeException ignored) {
        }
    }

    private String cacheKey(int limit, long accountId, long before) {
        return "v1:feed:listByFollowing:limit=" + limit + ":accountID=" + accountId + ":before=" + before;
    }
}
