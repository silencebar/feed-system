package com.example.feedsystem.comment.service;

import com.example.feedsystem.comment.dto.CommentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "feature", name = "async-event-enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class CommentEventConsumer {
    private final CommentEventHandler commentEventHandler;

    @RabbitListener(queues = "${comment.mq.queue:comment.events}")
    public void consume(CommentEvent event) {
        try {
            commentEventHandler.handle(event);
            log.info("Consumed comment event: eventId={}, action={}, commentId={}, videoId={}",
                    event.getEventId(), event.getAction(), event.getCommentId(), event.getVideoId());
        } catch (RuntimeException ex) {
            log.error("Failed to consume comment event: {}", event, ex);
            throw ex;
        }
    }
}
