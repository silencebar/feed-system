package com.example.feedsystem.comment.dto;

import com.example.feedsystem.comment.model.CommentDO;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentResponse {
    private Long id;
    private String username;
    @JsonProperty("video_id")
    private Long videoId;
    @JsonProperty("author_id")
    private Long authorId;
    private String content;
    @JsonProperty("created_at")
    private OffsetDateTime createdAt;

    public static CommentResponse from(CommentDO comment) {
        return new CommentResponse(comment.getId(), comment.getUsername(), comment.getVideoId(),
                comment.getAuthorId(), comment.getContent(), comment.getCreatedAt());
    }
}
