package com.example.feedsystem.account.mapper;

import com.example.feedsystem.account.model.AccountDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AccountMapper {

    AccountDO selectById(Long id);

    AccountDO selectByUsername(String username);

    AccountDO selectByRefreshToken(String refreshToken);

    int insert(AccountDO account);

    int updateLoginTokens(@Param("id") Long id, @Param("token") String token, @Param("refreshToken") String refreshToken);

    int updateToken(@Param("id") Long id, @Param("token") String token);

    int clearTokens(Long id);

    int updatePassword(@Param("id") Long id, @Param("password") String password);

    int updateUsernameAndToken(@Param("id") Long id, @Param("username") String username, @Param("token") String token);

    int updateProfile(@Param("id") Long id, @Param("avatarUrl") String avatarUrl, @Param("bio") String bio);
}
