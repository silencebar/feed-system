package com.example.feedsystem.comment.service;

import com.example.feedsystem.common.config.FeatureProperties;
import com.example.feedsystem.comment.dto.CommentEvent;
import com.example.feedsystem.comment.model.CommentDO;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CommentEventPublisher {
    private final RabbitTemplate rabbitTemplate;
    private final CommentEventHandler commentEventHandler;
    private final FeatureProperties featureProperties;

    @Value("${comment.mq.exchange:comment.events}")
    private String exchange;

    public void publishComment(CommentDO comment) { publish("comment.publish", "publish", comment); }
    public void publishDelete(CommentDO comment) { publish("comment.delete", "delete", comment); }

    private void publish(String routingKey, String action, CommentDO comment) {
        CommentEvent event = new CommentEvent(UUID.randomUUID().toString(), action, comment.getId(),
                comment.getVideoId(), comment.getAuthorId(), comment.getUsername(), comment.getContent(), Instant.now());
        if (!featureProperties.isAsyncEventEnabled()) {
            log.info("Async event disabled, handle event synchronously: {}", routingKey);
            try {
                commentEventHandler.handle(event);
            } catch (RuntimeException ex) {
                log.warn("Failed to handle comment event synchronously {}", routingKey, ex);
            }
            return;
        }
        log.info("Async event enabled, publish RabbitMQ event: {}", routingKey);
        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, event, message -> {
                message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                message.getMessageProperties().setContentType(MessageProperties.CONTENT_TYPE_JSON);
                return message;
            });
        } catch (AmqpException ex) {
            log.warn("Failed to publish comment event {}", routingKey, ex);
        }
    }
}
