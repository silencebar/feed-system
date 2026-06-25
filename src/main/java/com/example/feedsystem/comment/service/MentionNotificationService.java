package com.example.feedsystem.comment.service;

import com.example.feedsystem.account.mapper.AccountMapper;
import com.example.feedsystem.account.model.AccountDO;
import com.example.feedsystem.notification.service.NotificationWriteService;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MentionNotificationService {
    private static final Pattern MENTION_PATTERN = Pattern.compile("@([\\p{L}\\p{N}_-]+)");
    private final AccountMapper accountMapper;
    private final NotificationWriteService notificationWriteService;

    public void createNotifications(Long actorId, String actorUsername, Long videoId,
            Long videoAuthorId, String content) {
        if (!actorId.equals(videoAuthorId)) {
            notificationWriteService.create(videoAuthorId, actorId, "comment", videoId,
                    actorUsername + " commented on your video");
        }
        for (String username : extractMentions(content)) {
            AccountDO mentioned = accountMapper.selectByUsername(username);
            if (mentioned == null || mentioned.getId().equals(actorId)) {
                continue;
            }
            notificationWriteService.create(mentioned.getId(), actorId, "mention", videoId,
                    actorUsername + " mentioned you in a comment");
        }
    }

    private Set<String> extractMentions(String content) {
        Set<String> usernames = new LinkedHashSet<>();
        Matcher matcher = MENTION_PATTERN.matcher(content);
        while (matcher.find()) usernames.add(matcher.group(1));
        return usernames;
    }
}
