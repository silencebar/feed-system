package com.example.feedsystem.comment.service;

import com.example.feedsystem.account.mapper.AccountMapper;
import com.example.feedsystem.account.model.AccountDO;
import com.example.feedsystem.comment.dto.CommentResponse;
import com.example.feedsystem.comment.mapper.CommentMapper;
import com.example.feedsystem.comment.model.CommentDO;
import com.example.feedsystem.common.BusinessException;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CommentService {
    private static final int LIST_LIMIT = 200;
    private final CommentMapper commentMapper;
    private final AccountMapper accountMapper;
    private final CommentRateLimiter commentRateLimiter;
    private final CommentEventPublisher commentEventPublisher;

    @Transactional
    public void publish(Long accountId, Long videoId, String rawContent) {
        checkWriteRateLimit(accountId);
        String content = rawContent == null ? "" : rawContent.trim();
        if (content.isEmpty()) throw new BusinessException(HttpStatus.BAD_REQUEST, "content is required");
        requireVideo(videoId);
        AccountDO account = accountMapper.selectById(accountId);
        if (account == null) throw new BusinessException(HttpStatus.UNAUTHORIZED, "unauthorized");

        CommentDO comment = new CommentDO();
        comment.setUsername(account.getUsername());
        comment.setVideoId(videoId);
        comment.setAuthorId(accountId);
        comment.setContent(content);
        comment.setCreatedAt(OffsetDateTime.now());
        commentMapper.insertComment(comment);
        commentEventPublisher.publishComment(comment);
    }

    @Transactional
    public void delete(Long accountId, Long commentId) {
        checkWriteRateLimit(accountId);
        CommentDO comment = commentMapper.selectById(commentId);
        if (comment == null) throw new BusinessException(HttpStatus.NOT_FOUND, "comment not found");
        if (!accountId.equals(comment.getAuthorId())) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "unauthorized");
        }
        commentMapper.deleteById(commentId);
        commentEventPublisher.publishDelete(comment);
    }

    public List<CommentResponse> listAll(Long videoId) {
        requireVideo(videoId);
        return commentMapper.selectByVideoId(videoId, LIST_LIMIT).stream().map(CommentResponse::from).toList();
    }

    private void requireVideo(Long videoId) {
        if (!commentMapper.existsVideoById(videoId)) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "video not found");
        }
    }

    private void checkWriteRateLimit(Long accountId) {
        if (!commentRateLimiter.allowCommentWrite(accountId)) {
            throw new BusinessException(HttpStatus.TOO_MANY_REQUESTS, "too many requests");
        }
    }
}
