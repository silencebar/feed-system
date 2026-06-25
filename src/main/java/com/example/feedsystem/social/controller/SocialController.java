package com.example.feedsystem.social.controller;

import com.example.feedsystem.account.dto.MessageResponse;
import com.example.feedsystem.account.service.AuthenticatedAccount;
import com.example.feedsystem.social.dto.FollowRequest;
import com.example.feedsystem.social.dto.GetAllFollowersRequest;
import com.example.feedsystem.social.dto.GetAllFollowersResponse;
import com.example.feedsystem.social.dto.GetAllVloggersRequest;
import com.example.feedsystem.social.dto.GetAllVloggersResponse;
import com.example.feedsystem.social.dto.SocialCountsResponse;
import com.example.feedsystem.social.dto.UnfollowRequest;
import com.example.feedsystem.social.service.SocialService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/social")
@RequiredArgsConstructor
public class SocialController {

    private final SocialService socialService;

    @PostMapping("/follow")
    public MessageResponse follow(
            @RequestAttribute("account") AuthenticatedAccount account,
            @Valid @RequestBody FollowRequest request
    ) {
        socialService.follow(account.accountId(), request.getVloggerId());
        return new MessageResponse("followed");
    }

    @PostMapping("/unfollow")
    public MessageResponse unfollow(
            @RequestAttribute("account") AuthenticatedAccount account,
            @Valid @RequestBody UnfollowRequest request
    ) {
        socialService.unfollow(account.accountId(), request.getVloggerId());
        return new MessageResponse("unfollowed");
    }

    @PostMapping("/getAllFollowers")
    public GetAllFollowersResponse getAllFollowers(
            @RequestAttribute("account") AuthenticatedAccount account,
            @Valid @RequestBody(required = false) GetAllFollowersRequest request
    ) {
        return socialService.getAllFollowers(account.accountId(), request == null ? new GetAllFollowersRequest() : request);
    }

    @PostMapping("/getAllVloggers")
    public GetAllVloggersResponse getAllVloggers(
            @RequestAttribute("account") AuthenticatedAccount account,
            @Valid @RequestBody(required = false) GetAllVloggersRequest request
    ) {
        return socialService.getAllVloggers(account.accountId(), request == null ? new GetAllVloggersRequest() : request);
    }

    @PostMapping("/getCounts")
    public SocialCountsResponse getCounts(@RequestAttribute("account") AuthenticatedAccount account) {
        return socialService.getCounts(account.accountId());
    }
}
