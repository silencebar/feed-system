package com.example.feedsystem.storage;

import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "storage", name = "type", havingValue = "minio", matchIfMissing = true)
public class MinioConfig {
    private final StorageProperties storageProperties;

    @Bean
    public MinioClient minioClient() {
        StorageProperties.Minio minio = storageProperties.getMinio();
        return MinioClient.builder()
                .endpoint(minio.getEndpoint())
                .credentials(minio.getAccessKey(), minio.getSecretKey())
                .build();
    }
}
