package com.example.feedsystem.common.sse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SsePushService {

    public void pushToAccount(Long accountId, String eventName, Object payload) {
        if (accountId == null || accountId <= 0) return;
        log.debug("SSE push hook: accountId={}, eventName={}, payload={}", accountId, eventName, payload);
    }

    public void broadcast(String eventName, Object payload) {
        log.debug("SSE broadcast hook: eventName={}, payload={}", eventName, payload);
    }
}
