package com.example.feedsystem.like.service;

import com.example.feedsystem.like.dto.LikeEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "feature", name = "async-event-enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class LikeEventConsumer {
    private final LikeEventHandler likeEventHandler;

    @RabbitListener(queues = "${like.mq.queue:like.events}")
    public void consume(LikeEvent event) {
        try {
            likeEventHandler.handle(event);
            log.info("Consumed like event: eventId={}, action={}, accountId={}, videoId={}",
                    event.getEventId(), event.getAction(), event.getAccountId(), event.getVideoId());
        } catch (RuntimeException ex) {
            log.error("Failed to consume like event: {}", event, ex);
            throw ex;
        }
    }
}
