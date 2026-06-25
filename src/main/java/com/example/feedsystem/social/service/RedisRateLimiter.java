package com.example.feedsystem.social.service;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisRateLimiter {

    private static final long SOCIAL_WRITE_LIMIT = 20;
    private static final Duration SOCIAL_WRITE_WINDOW = Duration.ofMinutes(1);

    private final StringRedisTemplate redisTemplate;

    public boolean allowSocialWrite(Long accountId) {
        String key = "feedsystem:ratelimit:social_write:" + accountId;
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            redisTemplate.expire(key, SOCIAL_WRITE_WINDOW);
        }
        return count == null || count <= SOCIAL_WRITE_LIMIT;
    }
}
