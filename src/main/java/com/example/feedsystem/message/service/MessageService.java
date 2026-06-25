package com.example.feedsystem.message.service;

import com.example.feedsystem.common.BusinessException;
import com.example.feedsystem.message.dto.MessageListResponse;
import com.example.feedsystem.message.mapper.MessageMapper;
import com.example.feedsystem.message.model.MessageDO;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MessageService {
    private static final int CONVERSATION_LIMIT = 50;
    private final MessageMapper messageMapper;

    public MessageDO send(Long fromId, Long toId, String rawContent) {
        String content = rawContent == null ? "" : rawContent.trim();
        if (content.isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "content is required");
        }
        MessageDO message = new MessageDO();
        message.setFromId(fromId);
        message.setToId(toId);
        message.setContent(content);
        message.setRead(false);
        message.setCreatedAt(OffsetDateTime.now());
        messageMapper.insert(message);
        return message;
    }

    public MessageListResponse list(Long currentUserId, Long peerId) {
        List<MessageDO> messages = messageMapper.selectConversation(currentUserId, peerId, CONVERSATION_LIMIT);
        return new MessageListResponse(messages == null ? List.of() : messages);
    }
}
