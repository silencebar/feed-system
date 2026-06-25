package com.example.feedsystem.video.model;

import java.time.OffsetDateTime;
import lombok.Data;

@Data
public class OutboxMsgDO {

    private Long id;
    private Long videoId;
    private String eventType;
    private OffsetDateTime createTime;
    private String status;
}
