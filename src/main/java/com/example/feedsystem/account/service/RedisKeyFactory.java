package com.example.feedsystem.account.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RedisKeyFactory {

    private final String prefix;

    public RedisKeyFactory(@Value("${redis.key-prefix:v1:}") String prefix) {
        this.prefix = prefix;
    }

    public String accountAccessToken(Long accountId) {
        return prefix + "account:" + accountId;
    }

    public String accountRefreshToken(Long accountId) {
        return prefix + "account:" + accountId + ":refresh";
    }

    public String refreshLookup(String refreshToken) {
        return prefix + "refresh:" + refreshToken;
    }
}
