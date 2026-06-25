package com.example.feedsystem.social.dto;

import com.example.feedsystem.account.dto.AccountVO;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GetAllFollowersResponse {

    private List<AccountVO> followers;

    @JsonProperty("follower_count")
    private long followerCount;
}
