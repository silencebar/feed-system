package com.example.feedsystem.storage;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "storage")
public class StorageProperties {
    private String type = "minio";
    private Minio minio = new Minio();

    @Data
    public static class Minio {
        private boolean initialize = true;
        private String endpoint;
        private String accessKey;
        private String secretKey;
        private String bucket;
        private String publicUrl;
    }
}
