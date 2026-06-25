package com.example.feedsystem.message.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import lombok.Data;

@Data
public class MessageDO {
    private Long id;
    @JsonProperty("from_id")
    private Long fromId;
    @JsonProperty("to_id")
    private Long toId;
    private String content;
    @JsonProperty("is_read")
    private Boolean read;
    @JsonProperty("created_at")
    private OffsetDateTime createdAt;
}
