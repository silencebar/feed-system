package com.example.feedsystem.feed.service;

import com.example.feedsystem.common.config.FeatureProperties;
import com.example.feedsystem.feed.mapper.FeedMapper;
import com.example.feedsystem.video.model.VideoDO;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FeedVideoEntityCache {
    private static final Duration L1_TTL = Duration.ofSeconds(5);
    private static final Duration L2_TTL = Duration.ofHours(1);
    private final Map<Long, LocalEntry> localCache = new ConcurrentHashMap<>();
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final FeedMapper feedMapper;
    private final FeatureProperties featureProperties;

    public List<VideoDO> getByIds(List<Long> ids) {
        if (ids.isEmpty()) return List.of();
        if (!featureProperties.isRedisCacheEnabled()) {
            return feedMapper.selectByIds(ids);
        }
        Map<Long, VideoDO> found = new HashMap<>();
        List<Long> missing = new ArrayList<>();
        long now = System.nanoTime();
        for (Long id : ids) {
            LocalEntry entry = localCache.get(id);
            if (entry != null && entry.expiresAtNanos() > now) found.put(id, entry.video());
            else missing.add(id);
        }
        loadRedis(missing, found);
        List<Long> databaseIds = missing.stream().filter(id -> !found.containsKey(id)).toList();
        if (!databaseIds.isEmpty()) {
            List<VideoDO> databaseVideos = feedMapper.selectByIds(databaseIds);
            for (VideoDO video : databaseVideos) {
                found.put(video.getId(), video);
                saveCaches(video);
            }
        }
        return ids.stream().map(found::get).filter(java.util.Objects::nonNull).toList();
    }

    private void loadRedis(List<Long> ids, Map<Long, VideoDO> found) {
        if (ids.isEmpty()) return;
        try {
            List<String> values = redisTemplate.opsForValue().multiGet(ids.stream().map(this::key).toList());
            if (values == null) return;
            for (int i = 0; i < values.size(); i++) {
                String value = values.get(i);
                if (value == null) continue;
                VideoDO video = objectMapper.readValue(value, VideoDO.class);
                found.put(ids.get(i), video);
                saveLocal(video);
            }
        } catch (RuntimeException | java.io.IOException ignored) {
        }
    }

    private void saveCaches(VideoDO video) {
        saveLocal(video);
        try {
            redisTemplate.opsForValue().set(key(video.getId()), objectMapper.writeValueAsString(video), L2_TTL);
        } catch (RuntimeException | java.io.IOException ignored) {
        }
    }

    private void saveLocal(VideoDO video) {
        localCache.put(video.getId(), new LocalEntry(video, System.nanoTime() + L1_TTL.toNanos()));
    }

    private String key(Long id) {
        return "v1:video:entity:" + id;
    }

    private record LocalEntry(VideoDO video, long expiresAtNanos) {
    }
}
