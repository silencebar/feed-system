package com.example.feedsystem;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "jwt.secret=test-secret-test-secret-test-secret-32",
        "spring.datasource.url=jdbc:h2:mem:feed_system;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "storage.minio.initialize=false",
        "spring.rabbitmq.listener.simple.auto-startup=false"
})
class FeedSystemApplicationTests {

    @Test
    void contextLoads() {
    }

}
