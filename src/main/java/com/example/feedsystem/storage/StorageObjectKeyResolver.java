package com.example.feedsystem.storage;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class StorageObjectKeyResolver {
    private final StorageProperties storageProperties;

    public String toObjectKey(String value) {
        if (!StringUtils.hasText(value)) return value;
        String normalized = value.trim();
        String publicPrefix = trimRight(storageProperties.getMinio().getPublicUrl())
                + "/" + storageProperties.getMinio().getBucket() + "/";
        if (normalized.startsWith(publicPrefix)) {
            return normalized.substring(publicPrefix.length());
        }

        String endpointPrefix = trimRight(storageProperties.getMinio().getEndpoint())
                + "/" + storageProperties.getMinio().getBucket() + "/";
        if (normalized.startsWith(endpointPrefix)) {
            return normalized.substring(endpointPrefix.length());
        }
        return normalized;
    }

    private String trimRight(String value) {
        if (value == null || value.isBlank()) return "";
        int end = value.length();
        while (end > 0 && value.charAt(end - 1) == '/') end--;
        return value.substring(0, end);
    }
}
