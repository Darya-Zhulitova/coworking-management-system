package com.hse.adminservice.files;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "storage.s3")
public record FileStorageProperties(
        String endpoint,
        String publicBaseUrl,
        String accessKey,
        String secretKey,
        String bucket,
        String region
) {
}
