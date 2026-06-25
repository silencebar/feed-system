package com.example.feedsystem.dev;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Data
@Component
@Profile("dev")
@ConfigurationProperties(prefix = "data.seed")
public class DataSeedProperties {
    private boolean enabled = false;
    private boolean cleanup = false;
    private int userCount = 100;
    private int minVideosPerUser = 5;
    private int maxVideosPerUser = 10;
    private int minFollowsPerUser = 5;
    private int maxFollowsPerUser = 10;
    private int minLikesPerUser = 20;
    private int maxLikesPerUser = 50;
    private int minCommentsPerUser = 10;
    private int maxCommentsPerUser = 30;
    private String outputDir = "target/jmeter-data";
    private String password = "password123";
    private String videoObjectKey = "videos/seed/sample.mp4";
    private String coverObjectKey = "covers/seed/cover.jpg";
}
