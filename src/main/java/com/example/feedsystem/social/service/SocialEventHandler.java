package com.example.feedsystem.social.service;

import com.example.feedsystem.common.sse.SsePushService;
import com.example.feedsystem.notification.service.NotificationWriteService;
import com.example.feedsystem.social.dto.SocialEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SocialEventHandler {
    private final FeedCacheService feedCacheService;
    private final SsePushService ssePushService;
    private final NotificationWriteService notificationWriteService;

    public void handle(SocialEvent event) {
        if (event == null || event.getFollowerId() == null) {
            log.warn("Ignoring invalid social event: {}", event);
            return;
        }
        switch (event.getAction()) {
            case "follow", "unfollow" -> {
                feedCacheService.evictFollowingFeedCache(event.getFollowerId());
                if ("follow".equals(event.getAction())) {
                    notificationWriteService.create(event.getVloggerId(), event.getFollowerId(),
                            "follow", event.getFollowerId(), "followed you");
                }
                ssePushService.pushToAccount(event.getVloggerId(), "social." + event.getAction(), event);
            }
            default -> log.warn("Ignoring unknown social event action: eventId={}, action={}",
                    event.getEventId(), event.getAction());
        }
    }
}
