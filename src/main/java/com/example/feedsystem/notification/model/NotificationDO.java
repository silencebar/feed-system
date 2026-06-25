package com.example.feedsystem.notification.model;

import java.time.OffsetDateTime;
import lombok.Data;

@Data
public class NotificationDO {
    private Long id;
    private Long recipientId;
    private Long senderId;
    private String type;
    private Long targetId;
    private String content;
    private Boolean isRead;
    private OffsetDateTime createdAt;
}
