package com.example.feedsystem.comment.model;

import java.time.OffsetDateTime;
import lombok.Data;

@Data
public class CommentDO {
    private Long id;
    private String username;
    private Long videoId;
    private Long authorId;
    private String content;
    private OffsetDateTime createdAt;
}
