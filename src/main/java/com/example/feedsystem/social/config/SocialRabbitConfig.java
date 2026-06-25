package com.example.feedsystem.social.config;

import static com.example.feedsystem.common.config.RabbitDeadLetterConfig.DLX_EXCHANGE;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SocialRabbitConfig {

    @Bean
    public TopicExchange socialEventsExchange(@Value("${social.mq.exchange:social.events}") String exchange) {
        return new TopicExchange(exchange, true, false);
    }

    @Bean
    public Queue socialEventsQueue(@Value("${social.mq.queue:social.events}") String queue) {
        return QueueBuilder.durable(queue)
                .deadLetterExchange(DLX_EXCHANGE)
                .build();
    }

    @Bean
    public Binding socialEventsBinding(
            @Qualifier("socialEventsExchange") TopicExchange exchange,
            @Qualifier("socialEventsQueue") Queue queue
    ) {
        return BindingBuilder.bind(queue).to(exchange).with("social.*");
    }

    @Bean
    public Queue socialEventsDeadLetterQueue(@Value("${social.mq.queue:social.events}") String queue) {
        return QueueBuilder.durable(queue + ".dlx").build();
    }

    @Bean
    public Binding socialEventsDeadLetterBinding(
            @Qualifier("deadLetterExchange") TopicExchange exchange,
            @Qualifier("socialEventsDeadLetterQueue") Queue queue
    ) {
        return BindingBuilder.bind(queue).to(exchange).with("#");
    }

    @Bean
    public MessageConverter socialMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
