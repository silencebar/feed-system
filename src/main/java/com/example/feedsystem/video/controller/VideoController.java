package com.example.feedsystem.video.controller;

import com.example.feedsystem.account.dto.MessageResponse;
import com.example.feedsystem.account.service.AuthenticatedAccount;
import com.example.feedsystem.video.dto.DeleteVideoRequest;
import com.example.feedsystem.video.dto.GetDetailRequest;
import com.example.feedsystem.video.dto.ListByAuthorIdRequest;
import com.example.feedsystem.video.dto.PublishVideoRequest;
import com.example.feedsystem.video.dto.UploadResponse;
import com.example.feedsystem.video.dto.VideoResponse;
import com.example.feedsystem.video.service.VideoService;
import com.example.feedsystem.video.service.VideoUploadService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/video")
@RequiredArgsConstructor
public class VideoController {

    private final VideoUploadService videoUploadService;
    private final VideoService videoService;

    @PostMapping("/uploadVideo")
    public UploadResponse uploadVideo(
            @RequestAttribute("account") AuthenticatedAccount account,
            @RequestParam("file") MultipartFile file
    ) {
        return videoUploadService.uploadVideo(account.accountId(), file);
    }

    @PostMapping("/uploadCover")
    public UploadResponse uploadCover(
            @RequestAttribute("account") AuthenticatedAccount account,
            @RequestParam("file") MultipartFile file
    ) {
        return videoUploadService.uploadCover(account.accountId(), file);
    }

    @PostMapping("/publish")
    public VideoResponse publish(
            @RequestAttribute("account") AuthenticatedAccount account,
            @Valid @RequestBody PublishVideoRequest request
    ) {
        return videoService.publish(account.accountId(), account.username(), request);
    }

    @PostMapping("/delete")
    public MessageResponse delete(
            @RequestAttribute("account") AuthenticatedAccount account,
            @Valid @RequestBody DeleteVideoRequest request
    ) {
        videoService.delete(account.accountId(), request.getId());
        return new MessageResponse("video deleted");
    }

    @PostMapping("/listByAuthorID")
    public List<VideoResponse> listByAuthorId(@Valid @RequestBody ListByAuthorIdRequest request) {
        return videoService.listByAuthorId(request.getAuthorId());
    }

    @PostMapping("/getDetail")
    public VideoResponse getDetail(@Valid @RequestBody GetDetailRequest request) {
        return videoService.getDetail(request.getId());
    }
}
