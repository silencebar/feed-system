package com.example.feedsystem.like.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LikeEvent {

    @JsonProperty("event_id")
    private String eventId;

    private String action;

    @JsonProperty("account_id")
    private Long accountId;

    @JsonProperty("video_id")
    private Long videoId;

    @JsonProperty("occurred_at")
    private Instant occurredAt;
}
