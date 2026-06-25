package com.example.feedsystem.social.service;

import com.example.feedsystem.account.dto.AccountVO;
import com.example.feedsystem.account.mapper.AccountMapper;
import com.example.feedsystem.account.model.AccountDO;
import com.example.feedsystem.common.BusinessException;
import com.example.feedsystem.social.dto.GetAllFollowersRequest;
import com.example.feedsystem.social.dto.GetAllFollowersResponse;
import com.example.feedsystem.social.dto.GetAllVloggersRequest;
import com.example.feedsystem.social.dto.GetAllVloggersResponse;
import com.example.feedsystem.social.dto.SocialCountsResponse;
import com.example.feedsystem.social.mapper.SocialMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SocialService {

    private static final int LIST_LIMIT = 200;

    private final SocialMapper socialMapper;
    private final AccountMapper accountMapper;
    private final SocialEventPublisher socialEventPublisher;
    private final RedisRateLimiter redisRateLimiter;

    @Transactional
    public void follow(Long followerId, Long vloggerId) {
        checkWriteRateLimit(followerId);
        requireAccount(followerId);
        requireAccount(vloggerId);
        if (followerId.equals(vloggerId)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "can not follow self");
        }
        if (socialMapper.existsFollow(followerId, vloggerId)) {
            throw new BusinessException(HttpStatus.CONFLICT, "already followed");
        }

        try {
            socialMapper.insertFollow(followerId, vloggerId);
        } catch (DuplicateKeyException ex) {
            throw new BusinessException(HttpStatus.CONFLICT, "already followed");
        }

        socialEventPublisher.publishFollow(followerId, vloggerId);
    }

    @Transactional
    public void unfollow(Long followerId, Long vloggerId) {
        checkWriteRateLimit(followerId);
        requireAccount(followerId);
        requireAccount(vloggerId);
        if (!socialMapper.existsFollow(followerId, vloggerId)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "not followed");
        }

        socialMapper.deleteFollow(followerId, vloggerId);
        socialEventPublisher.publishUnfollow(followerId, vloggerId);
    }

    public GetAllFollowersResponse getAllFollowers(Long currentAccountId, GetAllFollowersRequest request) {
        Long vloggerId = request.getVloggerId() == null || request.getVloggerId() == 0
                ? currentAccountId : request.getVloggerId();
        requireAccount(vloggerId);
        List<AccountVO> followers = socialMapper.selectFollowerAccounts(vloggerId, LIST_LIMIT);
        long followerCount = socialMapper.countFollowers(vloggerId);
        return new GetAllFollowersResponse(followers, followerCount);
    }

    public GetAllVloggersResponse getAllVloggers(Long currentAccountId, GetAllVloggersRequest request) {
        Long followerId = request.getFollowerId() == null || request.getFollowerId() == 0
                ? currentAccountId : request.getFollowerId();
        requireAccount(followerId);
        List<AccountVO> vloggers = socialMapper.selectVloggerAccounts(followerId, LIST_LIMIT);
        long vloggerCount = socialMapper.countVloggers(followerId);
        return new GetAllVloggersResponse(vloggers, vloggerCount);
    }

    public SocialCountsResponse getCounts(Long accountId) {
        requireAccount(accountId);
        return new SocialCountsResponse(socialMapper.countFollowers(accountId), socialMapper.countVloggers(accountId));
    }

    private void checkWriteRateLimit(Long accountId) {
        if (!redisRateLimiter.allowSocialWrite(accountId)) {
            throw new BusinessException(HttpStatus.TOO_MANY_REQUESTS, "too many requests");
        }
    }

    private AccountDO requireAccount(Long accountId) {
        AccountDO account = accountMapper.selectById(accountId);
        if (account == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "account not found");
        }
        return account;
    }
}
