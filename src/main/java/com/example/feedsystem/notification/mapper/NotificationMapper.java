package com.example.feedsystem.notification.mapper;

import com.example.feedsystem.notification.model.NotificationDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface NotificationMapper {
    int insert(NotificationDO notification);

    Long selectVideoAuthorId(Long videoId);

    int deleteByVideoIds(@Param("videoIds") java.util.List<Long> videoIds);

    int deleteByAccountIds(@Param("accountIds") java.util.List<Long> accountIds);
}
