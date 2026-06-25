package com.example.feedsystem.comment.service;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CommentRateLimiter {
    private static final long LIMIT = 10;
    private static final Duration WINDOW = Duration.ofMinutes(1);
    private final StringRedisTemplate redisTemplate;

    public boolean allowCommentWrite(Long accountId) {
        String key = "feedsystem:ratelimit:comment_write:" + accountId;
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            redisTemplate.expire(key, WINDOW);
        }
        return count == null || count <= LIMIT;
    }
}
