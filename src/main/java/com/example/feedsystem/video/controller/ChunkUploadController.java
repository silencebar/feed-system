package com.example.feedsystem.video.controller;

import com.example.feedsystem.account.service.AuthenticatedAccount;
import com.example.feedsystem.video.dto.ChunkStatusRequest;
import com.example.feedsystem.video.dto.ChunkStatusResponse;
import com.example.feedsystem.video.dto.CompleteChunkUploadRequest;
import com.example.feedsystem.video.dto.InitChunkUploadRequest;
import com.example.feedsystem.video.dto.InitChunkUploadResponse;
import com.example.feedsystem.video.dto.UploadChunkResponse;
import com.example.feedsystem.video.dto.UploadResponse;
import com.example.feedsystem.video.service.ChunkUploadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/video/chunk")
@RequiredArgsConstructor
public class ChunkUploadController {

    private final ChunkUploadService chunkUploadService;

    @PostMapping("/init")
    public InitChunkUploadResponse init(
            @RequestAttribute("account") AuthenticatedAccount account,
            @Valid @RequestBody InitChunkUploadRequest request
    ) {
        return chunkUploadService.init(account.accountId(), request);
    }

    @PostMapping("/upload")
    public UploadChunkResponse upload(
            @RequestAttribute("account") AuthenticatedAccount account,
            @RequestParam("upload_id") String uploadId,
            @RequestParam("chunk_index") Integer chunkIndex,
            @RequestParam("chunk_hash") String chunkHash,
            @RequestParam("file") MultipartFile file
    ) {
        return new UploadChunkResponse(chunkUploadService.uploadChunk(account.accountId(), uploadId, chunkIndex, chunkHash, file));
    }

    @PostMapping("/status")
    public ChunkStatusResponse status(
            @RequestAttribute("account") AuthenticatedAccount account,
            @Valid @RequestBody ChunkStatusRequest request
    ) {
        return chunkUploadService.status(account.accountId(), request.getUploadId());
    }

    @PostMapping("/complete")
    public UploadResponse complete(
            @RequestAttribute("account") AuthenticatedAccount account,
            @Valid @RequestBody CompleteChunkUploadRequest request
    ) {
        return chunkUploadService.complete(account.accountId(), request.getUploadId());
    }
}
