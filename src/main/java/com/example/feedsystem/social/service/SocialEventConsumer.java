package com.example.feedsystem.social.service;

import com.example.feedsystem.social.dto.SocialEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "feature", name = "async-event-enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class SocialEventConsumer {
    private final SocialEventHandler socialEventHandler;

    @RabbitListener(queues = "${social.mq.queue:social.events}")
    public void consume(SocialEvent event) {
        try {
            socialEventHandler.handle(event);
            log.info("Consumed social event: eventId={}, action={}, followerId={}, vloggerId={}",
                    event.getEventId(), event.getAction(), event.getFollowerId(), event.getVloggerId());
        } catch (RuntimeException ex) {
            log.error("Failed to consume social event: {}", event, ex);
            throw ex;
        }
    }
}
