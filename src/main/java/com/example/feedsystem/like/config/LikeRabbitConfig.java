package com.example.feedsystem.like.config;

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
public class LikeRabbitConfig {

    @Bean
    public TopicExchange likeEventsExchange(@Value("${like.mq.exchange:like.events}") String exchange) {
        return new TopicExchange(exchange, true, false);
    }

    @Bean
    public Queue likeEventsQueue(@Value("${like.mq.queue:like.events}") String queue) {
        return QueueBuilder.durable(queue)
                .deadLetterExchange(DLX_EXCHANGE)
                .build();
    }

    @Bean
    public Binding likeEventsBinding(
            @Qualifier("likeEventsExchange") TopicExchange exchange,
            @Qualifier("likeEventsQueue") Queue queue
    ) {
        return BindingBuilder.bind(queue).to(exchange).with("like.*");
    }

    @Bean
    public Queue likeEventsDeadLetterQueue(@Value("${like.mq.queue:like.events}") String queue) {
        return QueueBuilder.durable(queue + ".dlx").build();
    }

    @Bean
    public Binding likeEventsDeadLetterBinding(
            @Qualifier("deadLetterExchange") TopicExchange exchange,
            @Qualifier("likeEventsDeadLetterQueue") Queue queue
    ) {
        return BindingBuilder.bind(queue).to(exchange).with("#");
    }
}
