package com.example.feedsystem.message.dto;

import com.example.feedsystem.message.model.MessageDO;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MessageListResponse {
    private List<MessageDO> messages;
}
