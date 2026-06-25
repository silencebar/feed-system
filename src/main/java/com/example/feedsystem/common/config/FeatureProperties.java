package com.example.feedsystem.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "feature")
public class FeatureProperties {
    private boolean redisCacheEnabled = true;
    private boolean asyncEventEnabled = true;
}
