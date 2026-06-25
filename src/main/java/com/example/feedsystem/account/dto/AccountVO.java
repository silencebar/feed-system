package com.example.feedsystem.account.dto;

import com.example.feedsystem.account.model.AccountDO;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountVO {

    private Long id;

    private String username;

    @JsonProperty("avatar_url")
    private String avatarUrl;

    private String bio;

    public static AccountVO from(AccountDO account) {
        return new AccountVO(account.getId(), account.getUsername(), account.getAvatarUrl(), account.getBio());
    }
}
