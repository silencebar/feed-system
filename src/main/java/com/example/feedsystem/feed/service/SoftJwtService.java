package com.example.feedsystem.feed.service;

import com.example.feedsystem.account.mapper.AccountMapper;
import com.example.feedsystem.account.model.AccountDO;
import com.example.feedsystem.account.service.AccountTokenCache;
import com.example.feedsystem.account.service.JwtService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SoftJwtService {
    private final JwtService jwtService;
    private final AccountTokenCache accountTokenCache;
    private final AccountMapper accountMapper;

    public long resolveViewerAccountId(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) return 0;
        String token = authorization.substring("Bearer ".length());
        try {
            Claims claims = jwtService.parseAccessToken(token);
            Number claim = claims.get("account_id", Number.class);
            if (claim == null) return 0;
            long accountId = claim.longValue();
            String cached = null;
            try {
                cached = accountTokenCache.getAccessToken(accountId);
            } catch (RuntimeException ignored) {
            }
            if (token.equals(cached)) return accountId;
            AccountDO account = accountMapper.selectById(accountId);
            return account != null && token.equals(account.getToken()) ? accountId : 0;
        } catch (RuntimeException ex) {
            return 0;
        }
    }
}
