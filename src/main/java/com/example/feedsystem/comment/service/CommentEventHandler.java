package com.example.feedsystem.comment.service;

import com.example.feedsystem.comment.dto.CommentEvent;
import com.example.feedsystem.comment.mapper.CommentMapper;
import com.example.feedsystem.common.sse.SsePushService;
import com.example.feedsystem.like.service.PopularityCacheService;
import com.example.feedsystem.video.service.VideoCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CommentEventHandler {
    private final CommentMapper commentMapper;
    private final MentionNotificationService mentionNotificationService;
    private final PopularityCacheService popularityCacheService;
    private final VideoCacheService videoCacheService;
    private final SsePushService ssePushService;

    public void handle(CommentEvent event) {
        if (event == null || event.getVideoId() == null) {
            log.warn("Ignoring invalid comment event: {}", event);
            return;
        }
        switch (event.getAction()) {
            case "publish" -> handlePublish(event);
            case "delete" -> handleDelete(event);
            default -> log.warn("Ignoring unknown comment event action: eventId={}, action={}",
                    event.getEventId(), event.getAction());
        }
    }

    private void handlePublish(CommentEvent event) {
        commentMapper.increaseVideoPopularity(event.getVideoId());
        mentionNotificationService.createNotifications(event.getAuthorId(), event.getUsername(), event.getVideoId(),
                commentMapper.selectVideoAuthorId(event.getVideoId()), event.getContent());
        videoCacheService.evictDetail(event.getVideoId());
        popularityCacheService.changeVideoPopularity(event.getVideoId(), 1);
        ssePushService.broadcast("comment.publish", event);
    }

    private void handleDelete(CommentEvent event) {
        videoCacheService.evictDetail(event.getVideoId());
        ssePushService.broadcast("comment.delete", event);
    }
}
