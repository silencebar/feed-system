package com.example.feedsystem.account.model;

import lombok.Data;

@Data
public class AccountDO {

    private Long id;

    private String username;

    private String password;

    private String token;

    private String refreshToken;

    private String avatarUrl;

    private String bio;
}
