package com.example.feedsystem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;

@EnableRabbit
@SpringBootApplication
public class FeedSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(FeedSystemApplication.class, args);
    }

}
