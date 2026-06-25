package com.example.feedsystem.feed.service;

import com.example.feedsystem.common.BusinessException;
import com.example.feedsystem.common.config.FeatureProperties;
import com.example.feedsystem.feed.dto.FeedVideoItem;
import com.example.feedsystem.feed.dto.LikesCountFeedResponse;
import com.example.feedsystem.feed.dto.ListByFollowingRequest;
import com.example.feedsystem.feed.dto.ListByPopularityRequest;
import com.example.feedsystem.feed.dto.ListByTagRequest;
import com.example.feedsystem.feed.dto.ListLatestRequest;
import com.example.feedsystem.feed.dto.ListLikesCountRequest;
import com.example.feedsystem.feed.dto.PopularityFeedResponse;
import com.example.feedsystem.feed.dto.TimelineFeedResponse;
import com.example.feedsystem.feed.dto.VideoListResponse;
import com.example.feedsystem.feed.mapper.FeedMapper;
import com.example.feedsystem.video.model.VideoDO;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeedService {
    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 50;
    private final FeedMapper feedMapper;
    private final FeedVideoAssembler assembler;
    private final TimelineService timelineService;
    private final FollowingFeedCacheService followingCache;
    private final PopularityFeedService popularityFeedService;
    private final FeatureProperties featureProperties;

    public TimelineFeedResponse listLatest(ListLatestRequest request, long viewerId) {
        int limit = normalizeLimit(request.getLimit());
        long beforeMillis = request.getLatestTime() == null ? 0 : request.getLatestTime();
        List<VideoDO> videos = timelineService.listLatest(beforeMillis, limit);
        List<FeedVideoItem> items = assembler.assemble(videos, viewerId);
        long next = videos.isEmpty() ? 0 : videos.get(videos.size() - 1).getCreateTime().toInstant().toEpochMilli();
        return new TimelineFeedResponse(items, next, videos.size() == limit);
    }

    public LikesCountFeedResponse listLikesCount(ListLikesCountRequest request, long viewerId) {
        if (featureProperties.isRedisCacheEnabled()) {
            log.info("Redis cache enabled: feed.listLikesCount uses MySQL likes_count ranking");
        } else {
            log.info("Redis cache disabled, fallback to MySQL: feed.listLikesCount");
        }
        int limit = normalizeLimit(request.getLimit());
        Long likesBefore = request.getLikesCountBefore();
        Long idBefore = request.getIdBefore();
        if ((likesBefore == null) != (idBefore == null)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "likes cursor fields must be provided together");
        }
        if (likesBefore != null && (likesBefore < 0 || idBefore < 0)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "invalid likes cursor");
        }
        if (Long.valueOf(0).equals(likesBefore) && Long.valueOf(0).equals(idBefore)) {
            likesBefore = null;
            idBefore = null;
        } else if (idBefore != null && idBefore <= 0) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "invalid likes cursor");
        }
        List<VideoDO> videos = feedMapper.selectByLikesCount(likesBefore, idBefore, limit);
        VideoDO last = videos.isEmpty() ? null : videos.get(videos.size() - 1);
        return new LikesCountFeedResponse(assembler.assemble(videos, viewerId),
                last == null ? null : last.getLikesCount(), last == null ? null : last.getId(),
                videos.size() == limit);
    }

    public TimelineFeedResponse listByFollowing(ListByFollowingRequest request, long accountId) {
        int limit = normalizeLimit(request.getLimit());
        long beforeSeconds = request.getLatestTime() == null ? 0 : request.getLatestTime();
        if (!featureProperties.isRedisCacheEnabled()) {
            log.info("Redis cache disabled, fallback to MySQL: feed.listByFollowing");
            OffsetDateTime before = beforeSeconds <= 0 ? null
                    : OffsetDateTime.ofInstant(Instant.ofEpochSecond(beforeSeconds), ZoneId.systemDefault());
            List<VideoDO> videos = feedMapper.selectByFollowing(accountId, before, limit);
            List<FeedVideoItem> items = assembler.assemble(videos, accountId);
            long next = videos.isEmpty() ? 0 : videos.get(videos.size() - 1).getCreateTime().toEpochSecond();
            return new TimelineFeedResponse(items, next, videos.size() == limit);
        }
        log.info("Redis cache enabled: feed.listByFollowing");
        TimelineFeedResponse cached = followingCache.get(limit, accountId, beforeSeconds);
        if (cached != null) return cached;
        boolean locked = followingCache.tryLock(limit, accountId, beforeSeconds);
        if (!locked) {
            for (int i = 0; i < 5; i++) {
                sleep20ms();
                cached = followingCache.get(limit, accountId, beforeSeconds);
                if (cached != null) return cached;
            }
        }
        try {
            OffsetDateTime before = beforeSeconds <= 0 ? null
                    : OffsetDateTime.ofInstant(Instant.ofEpochSecond(beforeSeconds), ZoneId.systemDefault());
            List<VideoDO> videos = feedMapper.selectByFollowing(accountId, before, limit);
            List<FeedVideoItem> items = assembler.assemble(videos, accountId);
            long next = videos.isEmpty() ? 0 : videos.get(videos.size() - 1).getCreateTime().toEpochSecond();
            TimelineFeedResponse response = new TimelineFeedResponse(items, next, videos.size() == limit);
            followingCache.save(limit, accountId, beforeSeconds, response);
            return response;
        } finally {
            if (locked) followingCache.unlock(limit, accountId, beforeSeconds);
        }
    }

    public PopularityFeedResponse listByPopularity(ListByPopularityRequest request, long viewerId) {
        validatePopularityCursor(request);
        return popularityFeedService.list(request, normalizeLimit(request.getLimit()), viewerId);
    }

    public VideoListResponse listByTag(ListByTagRequest request, long viewerId) {
        List<VideoDO> videos = feedMapper.selectByTag(request.getTagName().trim(), normalizeLimit(request.getLimit()));
        return new VideoListResponse(assembler.assemble(videos, viewerId));
    }

    private void validatePopularityCursor(ListByPopularityRequest request) {
        boolean any = request.getLatestPopularity() != null || request.getLatestBefore() != null
                || request.getLatestIdBefore() != null;
        boolean all = request.getLatestPopularity() != null && request.getLatestBefore() != null
                && request.getLatestIdBefore() != null;
        if (any && !all) throw new BusinessException(HttpStatus.BAD_REQUEST, "popularity cursor fields must be provided together");
        if (request.getLatestPopularity() != null && (request.getLatestPopularity() < 0 || request.getLatestIdBefore() <= 0)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "invalid popularity cursor");
        }
        if (request.getOffset() != null && request.getOffset() < 0) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "invalid offset");
        }
    }

    private int normalizeLimit(Integer limit) {
        return limit == null || limit < 1 || limit > MAX_LIMIT ? DEFAULT_LIMIT : limit;
    }

    private void sleep20ms() {
        try {
            Thread.sleep(20);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
