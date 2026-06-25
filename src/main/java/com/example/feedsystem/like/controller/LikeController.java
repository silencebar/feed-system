package com.example.feedsystem.like.controller;

import com.example.feedsystem.account.dto.MessageResponse;
import com.example.feedsystem.account.service.AuthenticatedAccount;
import com.example.feedsystem.like.dto.IsLikedResponse;
import com.example.feedsystem.like.dto.LikeRequest;
import com.example.feedsystem.like.service.LikeService;
import com.example.feedsystem.video.dto.VideoResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/like")
@RequiredArgsConstructor
public class LikeController {

    private final LikeService likeService;

    @PostMapping("/like")
    public MessageResponse like(
            @RequestAttribute("account") AuthenticatedAccount account,
            @Valid @RequestBody LikeRequest request
    ) {
        likeService.like(account.accountId(), request.getVideoId());
        return new MessageResponse("like success");
    }

    @PostMapping("/unlike")
    public MessageResponse unlike(
            @RequestAttribute("account") AuthenticatedAccount account,
            @Valid @RequestBody LikeRequest request
    ) {
        likeService.unlike(account.accountId(), request.getVideoId());
        return new MessageResponse("unlike success");
    }

    @PostMapping("/isLiked")
    public IsLikedResponse isLiked(
            @RequestAttribute("account") AuthenticatedAccount account,
            @Valid @RequestBody LikeRequest request
    ) {
        return new IsLikedResponse(likeService.isLiked(account.accountId(), request.getVideoId()));
    }

    @PostMapping("/listMyLikedVideos")
    public List<VideoResponse> listMyLikedVideos(@RequestAttribute("account") AuthenticatedAccount account) {
        return likeService.listMyLikedVideos(account.accountId());
    }
}
