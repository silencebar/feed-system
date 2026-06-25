package com.example.feedsystem.comment.controller;

import com.example.feedsystem.account.dto.MessageResponse;
import com.example.feedsystem.account.service.AuthenticatedAccount;
import com.example.feedsystem.comment.dto.CommentResponse;
import com.example.feedsystem.comment.dto.DeleteCommentRequest;
import com.example.feedsystem.comment.dto.ListCommentsRequest;
import com.example.feedsystem.comment.dto.PublishCommentRequest;
import com.example.feedsystem.comment.service.CommentService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/comment")
@RequiredArgsConstructor
public class CommentController {
    private final CommentService commentService;

    @PostMapping("/publish")
    public MessageResponse publish(@RequestAttribute("account") AuthenticatedAccount account,
            @Valid @RequestBody PublishCommentRequest request) {
        commentService.publish(account.accountId(), request.getVideoId(), request.getContent());
        return new MessageResponse("comment published successfully");
    }

    @PostMapping("/delete")
    public MessageResponse delete(@RequestAttribute("account") AuthenticatedAccount account,
            @Valid @RequestBody DeleteCommentRequest request) {
        commentService.delete(account.accountId(), request.getCommentId());
        return new MessageResponse("comment deleted successfully");
    }

    @PostMapping("/listAll")
    public List<CommentResponse> listAll(@Valid @RequestBody ListCommentsRequest request) {
        return commentService.listAll(request.getVideoId());
    }
}
