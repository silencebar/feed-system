package com.example.feedsystem.account.service;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AccountTokenCache {

    private final StringRedisTemplate redisTemplate;
    private final RedisKeyFactory redisKeyFactory;

    @Value("${jwt.access-token-cache-ttl:24h}")
    private Duration accessTokenCacheTtl;

    @Value("#{T(java.time.Duration).ofDays(${jwt.refresh-token-expire-days:7})}")
    private Duration refreshTokenTtl;

    public void saveLoginTokens(Long accountId, String accessToken, String refreshToken) {
        saveAccessToken(accountId, accessToken);
        redisTemplate.opsForValue().set(redisKeyFactory.accountRefreshToken(accountId), refreshToken, refreshTokenTtl);
        redisTemplate.opsForValue().set(redisKeyFactory.refreshLookup(refreshToken), accountId.toString(), refreshTokenTtl);
    }

    public void saveAccessToken(Long accountId, String accessToken) {
        redisTemplate.opsForValue().set(redisKeyFactory.accountAccessToken(accountId), accessToken, accessTokenCacheTtl);
    }

    public String getAccessToken(Long accountId) {
        return redisTemplate.opsForValue().get(redisKeyFactory.accountAccessToken(accountId));
    }

    public Long getAccountIdByRefreshToken(String refreshToken) {
        String accountId = redisTemplate.opsForValue().get(redisKeyFactory.refreshLookup(refreshToken));
        return accountId == null ? null : Long.valueOf(accountId);
    }

    public void deleteTokens(Long accountId, String refreshToken) {
        redisTemplate.delete(redisKeyFactory.accountAccessToken(accountId));
        redisTemplate.delete(redisKeyFactory.accountRefreshToken(accountId));
        if (refreshToken != null && !refreshToken.isBlank()) {
            redisTemplate.delete(redisKeyFactory.refreshLookup(refreshToken));
        }
    }
}
