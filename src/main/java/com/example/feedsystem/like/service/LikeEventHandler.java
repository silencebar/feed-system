package com.example.feedsystem.like.service;

import com.example.feedsystem.common.sse.SsePushService;
import com.example.feedsystem.like.dto.LikeEvent;
import com.example.feedsystem.notification.service.NotificationWriteService;
import com.example.feedsystem.video.service.VideoCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LikeEventHandler {
    private final PopularityCacheService popularityCacheService;
    private final VideoCacheService videoCacheService;
    private final SsePushService ssePushService;
    private final NotificationWriteService notificationWriteService;

    public void handle(LikeEvent event) {
        if (event == null || event.getVideoId() == null) {
            log.warn("Ignoring invalid like event: {}", event);
            return;
        }
        int change = switch (event.getAction()) {
            case "like" -> 1;
            case "unlike" -> -1;
            default -> 0;
        };
        if (change == 0) {
            log.warn("Ignoring unknown like event action: eventId={}, action={}",
                    event.getEventId(), event.getAction());
            return;
        }
        videoCacheService.evictDetail(event.getVideoId());
        popularityCacheService.changeVideoPopularity(event.getVideoId(), change);
        if (change > 0) {
            notificationWriteService.notifyVideoOwner(event.getAccountId(), event.getVideoId(),
                    "like", "liked your video");
        }
        ssePushService.broadcast("like." + event.getAction(), event);
    }
}
