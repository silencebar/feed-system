package com.example.feedsystem.feed.service;

import com.example.feedsystem.common.config.FeatureProperties;
import com.example.feedsystem.feed.mapper.FeedMapper;
import com.example.feedsystem.video.model.VideoDO;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TimelineService {
    private static final String TIMELINE_KEY = "v1:feed:global_timeline";
    private static final int TIMELINE_LIMIT = 1000;
    private final StringRedisTemplate redisTemplate;
    private final FeedMapper feedMapper;
    private final FeedVideoEntityCache videoEntityCache;
    private final FeatureProperties featureProperties;

    public List<VideoDO> listLatest(long beforeMillis, int limit) {
        if (!featureProperties.isRedisCacheEnabled()) {
            log.info("Redis cache disabled, fallback to MySQL: feed.listLatest");
            return feedMapper.selectLatest(millisToTime(beforeMillis), limit);
        }
        log.info("Redis cache enabled: feed.listLatest");
        try {
            ensureTimeline(limit);
            double max = beforeMillis > 0 ? beforeMillis - 1 : Double.POSITIVE_INFINITY;
            Set<String> members = redisTemplate.opsForZSet()
                    .reverseRangeByScore(TIMELINE_KEY, 0, max, 0, limit);
            List<VideoDO> hot = fetchInOrder(members);
            if (hot.size() >= limit) return hot.stream().limit(limit).toList();

            List<VideoDO> cold = feedMapper.selectLatest(millisToTime(beforeMillis), limit + hot.size());
            Map<Long, VideoDO> merged = new LinkedHashMap<>();
            hot.forEach(video -> merged.put(video.getId(), video));
            cold.forEach(video -> merged.putIfAbsent(video.getId(), video));
            return merged.values().stream()
                    .sorted(Comparator.comparing(VideoDO::getCreateTime).reversed()
                            .thenComparing(VideoDO::getId, Comparator.reverseOrder()))
                    .limit(limit)
                    .toList();
        } catch (RuntimeException ex) {
            return feedMapper.selectLatest(millisToTime(beforeMillis), limit);
        }
    }

    public void addVideo(VideoDO video) {
        if (!featureProperties.isRedisCacheEnabled()) {
            log.info("Redis cache disabled, skip timeline update");
            return;
        }
        try {
            redisTemplate.opsForZSet().add(TIMELINE_KEY, video.getId().toString(),
                    video.getCreateTime().toInstant().toEpochMilli());
            trimTimeline();
        } catch (RuntimeException ignored) {
        }
    }

    private void ensureTimeline(int requestedLimit) {
        Long size = redisTemplate.opsForZSet().size(TIMELINE_KEY);
        List<VideoDO> latest = feedMapper.selectLatest(null, TIMELINE_LIMIT);
        if (latest.isEmpty()) return;
        if (size != null && size >= Math.min(requestedLimit, latest.size())) return;

        redisTemplate.delete(TIMELINE_KEY);
        for (VideoDO video : latest) {
            redisTemplate.opsForZSet().add(TIMELINE_KEY, video.getId().toString(),
                    video.getCreateTime().toInstant().toEpochMilli());
        }
        trimTimeline();
    }

    private void trimTimeline() {
        Long current = redisTemplate.opsForZSet().size(TIMELINE_KEY);
        if (current != null && current > TIMELINE_LIMIT) {
            redisTemplate.opsForZSet().removeRange(TIMELINE_KEY, 0, current - TIMELINE_LIMIT - 1);
        }
    }

    private List<VideoDO> fetchInOrder(Set<String> members) {
        if (members == null || members.isEmpty()) return List.of();
        List<Long> ids = members.stream().map(Long::valueOf).toList();
        return videoEntityCache.getByIds(ids);
    }

    private OffsetDateTime millisToTime(long millis) {
        return millis <= 0 ? null : OffsetDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault());
    }
}
