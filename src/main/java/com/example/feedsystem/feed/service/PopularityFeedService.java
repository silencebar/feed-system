package com.example.feedsystem.feed.service;

import com.example.feedsystem.common.config.FeatureProperties;
import com.example.feedsystem.feed.dto.ListByPopularityRequest;
import com.example.feedsystem.feed.dto.PopularityFeedResponse;
import com.example.feedsystem.feed.dto.FeedVideoItem;
import com.example.feedsystem.feed.mapper.FeedMapper;
import com.example.feedsystem.video.model.VideoDO;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PopularityFeedService {
    private static final DateTimeFormatter MINUTE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmm");
    private final StringRedisTemplate redisTemplate;
    private final FeedMapper feedMapper;
    private final FeedVideoAssembler assembler;
    private final FeedVideoEntityCache videoEntityCache;
    private final FeatureProperties featureProperties;

    public PopularityFeedResponse list(ListByPopularityRequest request, int limit, long viewerId) {
        if (!featureProperties.isRedisCacheEnabled()) {
            log.info("Redis cache disabled, fallback to MySQL: feed.listByPopularity");
            return listMysql(request, limit, viewerId);
        }
        log.info("Redis cache enabled: feed.listByPopularity");
        long offset = request.getOffset() == null ? 0 : request.getOffset();
        long asOf = request.getAsOf() == null || request.getAsOf() == 0
                ? Instant.now().truncatedTo(ChronoUnit.MINUTES).getEpochSecond() : request.getAsOf();
        try {
            List<VideoDO> videos = listRedis(asOf, offset, limit);
            if (!videos.isEmpty() || offset > 0) {
                List<FeedVideoItem> items = assembler.assemble(videos, viewerId);
                return new PopularityFeedResponse(items, asOf, offset + videos.size(),
                        videos.size() == limit, null, null, null);
            }
        } catch (RuntimeException ignored) {
        }
        return listMysql(request, limit, viewerId);
    }

    private List<VideoDO> listRedis(long asOf, long offset, int limit) {
        OffsetDateTime minute = OffsetDateTime.ofInstant(Instant.ofEpochSecond(asOf), ZoneId.systemDefault())
                .truncatedTo(ChronoUnit.MINUTES);
        String snapshot = "v1:hot:video:merge:1m:" + minute.format(MINUTE_FORMAT);
        if (!Boolean.TRUE.equals(redisTemplate.hasKey(snapshot))) {
            List<String> keys = new ArrayList<>();
            keys.add(snapshot);
            for (int i = 0; i < 60; i++) {
                keys.add("v1:hot:video:1m:" + minute.minusMinutes(i).format(MINUTE_FORMAT));
            }
            String lua = "return redis.call('ZUNIONSTORE', KEYS[1], #KEYS - 1, unpack(KEYS, 2))";
            redisTemplate.execute(new DefaultRedisScript<>(lua, Long.class), keys);
            redisTemplate.expire(snapshot, java.time.Duration.ofMinutes(2));
        }
        Set<String> members = redisTemplate.opsForZSet().reverseRange(snapshot, offset, offset + limit - 1);
        if (members == null || members.isEmpty()) return List.of();
        List<Long> ids = members.stream().map(Long::valueOf).toList();
        return videoEntityCache.getByIds(ids);
    }

    private PopularityFeedResponse listMysql(ListByPopularityRequest request, int limit, long viewerId) {
        List<VideoDO> videos = feedMapper.selectByPopularity(request.getLatestPopularity(),
                request.getLatestBefore(), request.getLatestIdBefore(), limit);
        List<FeedVideoItem> items = assembler.assemble(videos, viewerId);
        VideoDO last = videos.isEmpty() ? null : videos.get(videos.size() - 1);
        return new PopularityFeedResponse(items, 0, 0, videos.size() == limit,
                last == null ? null : last.getPopularity(),
                last == null ? null : last.getCreateTime(),
                last == null ? null : last.getId());
    }
}
