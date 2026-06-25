package com.example.feedsystem.comment.config;

import static com.example.feedsystem.common.config.RabbitDeadLetterConfig.DLX_EXCHANGE;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CommentRabbitConfig {

    private static final String COMMENT_BINDING_KEY = "comment.*";
    @Bean
    public TopicExchange commentEventsExchange(@Value("${comment.mq.exchange:comment.events}") String exchange) {
        return new TopicExchange(exchange, true, false);
    }

    @Bean
    public Queue commentEventsQueue(@Value("${comment.mq.queue:comment.events}") String queue) {
        return QueueBuilder.durable(queue)
                .deadLetterExchange(DLX_EXCHANGE)
                .build();
    }

    @Bean
    public Binding commentEventsBinding(
            @Qualifier("commentEventsExchange") TopicExchange exchange,
            @Qualifier("commentEventsQueue") Queue queue
    ) {
        return BindingBuilder.bind(queue).to(exchange).with(COMMENT_BINDING_KEY);
    }

    @Bean
    public Queue commentEventsDeadLetterQueue(@Value("${comment.mq.queue:comment.events}") String queue) {
        return QueueBuilder.durable(queue + ".dlx").build();
    }

    @Bean
    public Binding commentEventsDeadLetterBinding(
            @Qualifier("deadLetterExchange") TopicExchange exchange,
            @Qualifier("commentEventsDeadLetterQueue") Queue queue
    ) {
        return BindingBuilder.bind(queue).to(exchange).with("#");
    }
}
