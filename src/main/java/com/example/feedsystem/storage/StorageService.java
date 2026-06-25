package com.example.feedsystem.storage;

import org.springframework.web.multipart.MultipartFile;

public interface StorageService {
    void upload(MultipartFile file, String objectKey);

    void delete(String objectKey);

    String getUrl(String objectKey);
}
