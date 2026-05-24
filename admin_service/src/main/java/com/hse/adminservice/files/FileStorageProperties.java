package com.hse.adminservice.files;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "storage.s3")
public record FileStorageProperties(
        String endpoint,
        String accessKey,
        String secretKey,
        String bucket,
        String region,
        Integer presignedUrlTtlMinutes
) {
    public int effectivePresignedUrlTtlMinutes() {
        return presignedUrlTtlMinutes == null || presignedUrlTtlMinutes <= 0 ? 60 : presignedUrlTtlMinutes;
    }
}
