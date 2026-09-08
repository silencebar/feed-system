package com.example.feedsystem.creatorassistant.controller;

import com.example.feedsystem.creatorassistant.dto.CreatorSuggestRequest;
import com.example.feedsystem.creatorassistant.dto.CreatorSuggestResponse;
import com.example.feedsystem.creatorassistant.service.CreatorAssistantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/creator-assistant")
@RequiredArgsConstructor
public class CreatorAssistantController {
    private final CreatorAssistantService creatorAssistantService;

    @PostMapping("/suggest")
    public CreatorSuggestResponse suggest(@Valid @RequestBody CreatorSuggestRequest request) {
        return creatorAssistantService.suggest(request);
    }
}
