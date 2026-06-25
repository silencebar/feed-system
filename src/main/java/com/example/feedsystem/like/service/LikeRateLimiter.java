package com.example.feedsystem.like.service;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LikeRateLimiter {

    private static final long LIKE_WRITE_LIMIT = 30;
    private static final Duration LIKE_WRITE_WINDOW = Duration.ofMinutes(1);

    private final StringRedisTemplate redisTemplate;

    public boolean allowLikeWrite(Long accountId) {
        String key = "feedsystem:ratelimit:like_write:" + accountId;
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            redisTemplate.expire(key, LIKE_WRITE_WINDOW);
        }
        return count == null || count <= LIKE_WRITE_LIMIT;
    }
}
