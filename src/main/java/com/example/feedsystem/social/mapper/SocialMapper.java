package com.example.feedsystem.social.mapper;

import com.example.feedsystem.account.dto.AccountVO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SocialMapper {

    boolean existsFollow(@Param("followerId") Long followerId, @Param("vloggerId") Long vloggerId);

    int insertFollow(@Param("followerId") Long followerId, @Param("vloggerId") Long vloggerId);

    int deleteFollow(@Param("followerId") Long followerId, @Param("vloggerId") Long vloggerId);

    long countFollowers(Long vloggerId);

    long countVloggers(Long followerId);

    List<AccountVO> selectFollowerAccounts(@Param("vloggerId") Long vloggerId, @Param("limit") int limit);

    List<AccountVO> selectVloggerAccounts(@Param("followerId") Long followerId, @Param("limit") int limit);
}
