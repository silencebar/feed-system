package com.example.feedsystem.common.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitDeadLetterConfig {

    public static final String DLX_EXCHANGE = "dlx.events";

    @Bean
    public TopicExchange deadLetterExchange() {
        return new TopicExchange(DLX_EXCHANGE, true, false);
    }
}
