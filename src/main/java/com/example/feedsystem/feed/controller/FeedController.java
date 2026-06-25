package com.example.feedsystem.feed.controller;

import com.example.feedsystem.account.service.AuthenticatedAccount;
import com.example.feedsystem.feed.dto.LikesCountFeedResponse;
import com.example.feedsystem.feed.dto.ListByFollowingRequest;
import com.example.feedsystem.feed.dto.ListByPopularityRequest;
import com.example.feedsystem.feed.dto.ListByTagRequest;
import com.example.feedsystem.feed.dto.ListLatestRequest;
import com.example.feedsystem.feed.dto.ListLikesCountRequest;
import com.example.feedsystem.feed.dto.PopularityFeedResponse;
import com.example.feedsystem.feed.dto.TimelineFeedResponse;
import com.example.feedsystem.feed.dto.VideoListResponse;
import com.example.feedsystem.feed.service.FeedService;
import com.example.feedsystem.feed.service.SoftJwtService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/feed")
@RequiredArgsConstructor
public class FeedController {
    private final FeedService feedService;
    private final SoftJwtService softJwtService;

    @PostMapping("/listLatest")
    public TimelineFeedResponse listLatest(@RequestBody(required = false) ListLatestRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return feedService.listLatest(request == null ? new ListLatestRequest() : request,
                softJwtService.resolveViewerAccountId(authorization));
    }

    @PostMapping("/listLikesCount")
    public LikesCountFeedResponse listLikesCount(@RequestBody(required = false) ListLikesCountRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return feedService.listLikesCount(request == null ? new ListLikesCountRequest() : request,
                softJwtService.resolveViewerAccountId(authorization));
    }

    @PostMapping("/listByFollowing")
    public TimelineFeedResponse listByFollowing(@RequestAttribute("account") AuthenticatedAccount account,
            @RequestBody(required = false) ListByFollowingRequest request) {
        return feedService.listByFollowing(request == null ? new ListByFollowingRequest() : request, account.accountId());
    }

    @PostMapping("/listByPopularity")
    public PopularityFeedResponse listByPopularity(@RequestBody(required = false) ListByPopularityRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return feedService.listByPopularity(request == null ? new ListByPopularityRequest() : request,
                softJwtService.resolveViewerAccountId(authorization));
    }

    @PostMapping("/listByTag")
    public VideoListResponse listByTag(@Valid @RequestBody ListByTagRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return feedService.listByTag(request, softJwtService.resolveViewerAccountId(authorization));
    }
}
