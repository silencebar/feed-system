package com.example.feedsystem.notification.service;

import com.example.feedsystem.common.sse.SsePushService;
import com.example.feedsystem.notification.mapper.NotificationMapper;
import com.example.feedsystem.notification.model.NotificationDO;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationWriteService {
    private final NotificationMapper notificationMapper;
    private final SsePushService ssePushService;

    public void notifyVideoOwner(Long senderId, Long videoId, String type, String content) {
        Long recipientId = notificationMapper.selectVideoAuthorId(videoId);
        if (recipientId == null || recipientId.equals(senderId)) {
            return;
        }
        create(recipientId, senderId, type, videoId, content);
    }

    public void create(Long recipientId, Long senderId, String type, Long targetId, String content) {
        if (recipientId == null || senderId == null || recipientId.equals(senderId)) {
            return;
        }
        NotificationDO notification = new NotificationDO();
        notification.setRecipientId(recipientId);
        notification.setSenderId(senderId);
        notification.setType(type);
        notification.setTargetId(targetId);
        notification.setContent(content);
        notification.setIsRead(false);
        notification.setCreatedAt(OffsetDateTime.now());
        notificationMapper.insert(notification);
        ssePushService.pushToAccount(recipientId, "notification." + type, notification);
    }
}
