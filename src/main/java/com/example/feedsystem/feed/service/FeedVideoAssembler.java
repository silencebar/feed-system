package com.example.feedsystem.feed.service;

import com.example.feedsystem.feed.dto.FeedVideoItem;
import com.example.feedsystem.feed.mapper.FeedMapper;
import com.example.feedsystem.storage.StorageService;
import com.example.feedsystem.video.model.VideoDO;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FeedVideoAssembler {
    private final FeedMapper feedMapper;
    private final StorageService storageService;

    public List<FeedVideoItem> assemble(List<VideoDO> videos, long viewerAccountId) {
        List<FeedVideoItem> items = videos.stream()
                .map(video -> FeedVideoItem.from(video, storageService))
                .toList();
        if (viewerAccountId <= 0 || videos.isEmpty()) return items;
        List<Long> ids = videos.stream().map(VideoDO::getId).toList();
        Set<Long> likedIds = new HashSet<>(feedMapper.selectLikedVideoIds(viewerAccountId, ids));
        items.forEach(item -> item.setLiked(likedIds.contains(item.getId())));
        return items;
    }
}
