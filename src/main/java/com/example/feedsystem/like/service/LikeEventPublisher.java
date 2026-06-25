package com.example.feedsystem.like.service;

import com.example.feedsystem.common.config.FeatureProperties;
import com.example.feedsystem.like.dto.LikeEvent;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LikeEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final LikeEventHandler likeEventHandler;
    private final FeatureProperties featureProperties;

    @Value("${like.mq.exchange:like.events}")
    private String exchange;

    public void publishLike(Long accountId, Long videoId) {
        publish("like.like", "like", accountId, videoId);
    }

    public void publishUnlike(Long accountId, Long videoId) {
        publish("like.unlike", "unlike", accountId, videoId);
    }

    private void publish(String routingKey, String action, Long accountId, Long videoId) {
        LikeEvent event = new LikeEvent(UUID.randomUUID().toString(), action, accountId, videoId, Instant.now());
        if (!featureProperties.isAsyncEventEnabled()) {
            log.info("Async event disabled, handle event synchronously: {}", routingKey);
            try {
                likeEventHandler.handle(event);
            } catch (RuntimeException ex) {
                log.warn("Failed to handle like event synchronously {}", routingKey, ex);
            }
            return;
        }
        log.info("Async event enabled, publish RabbitMQ event: {}", routingKey);
        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, event);
        } catch (AmqpException ex) {
            log.warn("Failed to publish like event {}", routingKey, ex);
        }
    }
}
