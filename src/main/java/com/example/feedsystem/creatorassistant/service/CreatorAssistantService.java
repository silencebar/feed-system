package com.example.feedsystem.creatorassistant.service;

import com.example.feedsystem.creatorassistant.client.CreatorAgentClient;
import com.example.feedsystem.creatorassistant.dto.CreatorSuggestRequest;
import com.example.feedsystem.creatorassistant.dto.CreatorSuggestResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CreatorAssistantService {
    private final CreatorAgentClient creatorAgentClient;

    public CreatorSuggestResponse suggest(CreatorSuggestRequest request) {
        return creatorAgentClient.suggest(request);
    }
}
