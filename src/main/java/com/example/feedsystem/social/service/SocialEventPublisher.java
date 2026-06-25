package com.example.feedsystem.social.service;

import com.example.feedsystem.common.config.FeatureProperties;
import com.example.feedsystem.social.dto.SocialEvent;
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
public class SocialEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final SocialEventHandler socialEventHandler;
    private final FeatureProperties featureProperties;

    @Value("${social.mq.exchange:social.events}")
    private String exchange;

    public void publishFollow(Long followerId, Long vloggerId) {
        publish("social.follow", "follow", followerId, vloggerId);
    }

    public void publishUnfollow(Long followerId, Long vloggerId) {
        publish("social.unfollow", "unfollow", followerId, vloggerId);
    }

    private void publish(String routingKey, String action, Long followerId, Long vloggerId) {
        SocialEvent event = new SocialEvent(UUID.randomUUID().toString(), action, followerId, vloggerId, Instant.now());
        if (!featureProperties.isAsyncEventEnabled()) {
            log.info("Async event disabled, handle event synchronously: {}", routingKey);
            try {
                socialEventHandler.handle(event);
            } catch (RuntimeException ex) {
                log.warn("Failed to handle social event synchronously {}", routingKey, ex);
            }
            return;
        }
        log.info("Async event enabled, publish RabbitMQ event: {}", routingKey);
        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, event);
        } catch (AmqpException ex) {
            log.warn("Failed to publish social event {}", routingKey, ex);
        }
    }
}
