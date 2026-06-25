package com.example.feedsystem.social.service;

import com.example.feedsystem.common.config.FeatureProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeedCacheService {

    private final StringRedisTemplate redisTemplate;
    private final FeatureProperties featureProperties;

    public void evictFollowingFeedCache(Long accountId) {
        if (!featureProperties.isRedisCacheEnabled()) {
            log.info("Redis cache disabled, skip following feed cache eviction");
            return;
        }
        String pattern = "v1:feed:listByFollowing:*:accountID=" + accountId + ":*";
        ScanOptions options = ScanOptions.scanOptions().match(pattern).count(200).build();
        redisTemplate.execute((RedisCallback<Void>) connection -> {
            try (Cursor<byte[]> cursor = connection.keyCommands().scan(options)) {
                while (cursor.hasNext()) {
                    connection.keyCommands().del(cursor.next());
                }
            }
            return null;
        });
    }
}
