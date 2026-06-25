package com.example.feedsystem.social.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SocialEvent {

    @JsonProperty("event_id")
    private String eventId;

    private String action;

    @JsonProperty("follower_id")
    private Long followerId;

    @JsonProperty("vlogger_id")
    private Long vloggerId;

    @JsonProperty("occurred_at")
    private Instant occurredAt;
}
