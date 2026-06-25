package com.example.feedsystem.message.controller;

import com.example.feedsystem.account.service.AuthenticatedAccount;
import com.example.feedsystem.message.dto.ListMessagesRequest;
import com.example.feedsystem.message.dto.MessageListResponse;
import com.example.feedsystem.message.dto.SendMessageRequest;
import com.example.feedsystem.message.model.MessageDO;
import com.example.feedsystem.message.service.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/message")
@RequiredArgsConstructor
public class MessageController {
    private final MessageService messageService;

    @PostMapping("/send")
    public MessageDO send(@RequestAttribute("account") AuthenticatedAccount account,
            @Valid @RequestBody SendMessageRequest request) {
        return messageService.send(account.accountId(), request.getToId(), request.getContent());
    }

    @PostMapping("/list")
    public MessageListResponse list(@RequestAttribute("account") AuthenticatedAccount account,
            @Valid @RequestBody ListMessagesRequest request) {
        return messageService.list(account.accountId(), request.getPeerId());
    }
}
