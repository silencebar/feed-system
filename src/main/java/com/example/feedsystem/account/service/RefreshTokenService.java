package com.example.feedsystem.account.service;

import java.security.SecureRandom;
import java.util.HexFormat;
import org.springframework.stereotype.Service;

@Service
public class RefreshTokenService {

    private static final int TOKEN_BYTES = 32;

    private final SecureRandom secureRandom = new SecureRandom();

    public String generateRefreshToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }
}
