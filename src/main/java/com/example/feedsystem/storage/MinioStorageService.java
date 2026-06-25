package com.example.feedsystem.storage;

import com.example.feedsystem.common.BusinessException;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.SetBucketPolicyArgs;
import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "storage", name = "type", havingValue = "minio", matchIfMissing = true)
public class MinioStorageService implements StorageService {
    private final MinioClient minioClient;
    private final StorageProperties storageProperties;

    @PostConstruct
    public void ensureBucket() {
        if (!minio().isInitialize()) {
            return;
        }
        String bucket = minio().getBucket();
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            }
            minioClient.setBucketPolicy(SetBucketPolicyArgs.builder()
                    .bucket(bucket)
                    .config(publicReadPolicy(bucket))
                    .build());
        } catch (Exception ex) {
            throw new IllegalStateException("failed to initialize MinIO bucket: " + bucket, ex);
        }
    }

    @Override
    public void upload(MultipartFile file, String objectKey) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "file is required");
        }
        try (InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(minio().getBucket())
                    .object(objectKey)
                    .stream(inputStream, file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());
        } catch (Exception ex) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "failed to upload file");
        }
    }

    @Override
    public void delete(String objectKey) {
        if (!StringUtils.hasText(objectKey)) return;
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(minio().getBucket())
                    .object(objectKey)
                    .build());
        } catch (Exception ignored) {
        }
    }

    @Override
    public String getUrl(String objectKey) {
        if (!StringUtils.hasText(objectKey)) return objectKey;
        if (objectKey.startsWith("http://") || objectKey.startsWith("https://") || objectKey.startsWith("/")) {
            return objectKey;
        }
        return trimRight(minio().getPublicUrl()) + "/" + minio().getBucket() + "/" + objectKey;
    }

    private StorageProperties.Minio minio() {
        return storageProperties.getMinio();
    }

    private String trimRight(String value) {
        if (value == null || value.isBlank()) return "";
        int end = value.length();
        while (end > 0 && value.charAt(end - 1) == '/') end--;
        return value.substring(0, end);
    }

    private String publicReadPolicy(String bucket) {
        return """
                {
                  "Version": "2012-10-17",
                  "Statement": [
                    {
                      "Effect": "Allow",
                      "Principal": {"AWS": ["*"]},
                      "Action": ["s3:GetObject"],
                      "Resource": ["arn:aws:s3:::%s/*"]
                    }
                  ]
                }
                """.formatted(bucket);
    }
}
