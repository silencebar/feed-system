package com.example.feedsystem.comment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentEvent {
    @JsonProperty("event_id")
    private String eventId;
    private String action;
    @JsonProperty("comment_id")
    private Long commentId;
    @JsonProperty("video_id")
    private Long videoId;
    @JsonProperty("author_id")
    private Long authorId;
    private String username;
    private String content;
    @JsonProperty("occurred_at")
    private Instant occurredAt;
}
