package com.example.feedsystem.like.model;

import java.time.OffsetDateTime;
import lombok.Data;

@Data
public class LikeDO {

    private Long id;
    private Long videoId;
    private Long accountId;
    private OffsetDateTime createdAt;
}
