package com.hse.adminservice.files;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class FileStorageService {
    private final S3Presigner s3Presigner;
    private final FileStorageProperties properties;

    public String presignedUrl(String fileId) {
        if (!StringUtils.hasText(fileId)) {
            return null;
        }

        GetObjectRequest getObjectRequest = GetObjectRequest.builder().bucket(properties.bucket()).key(fileId).build();
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder().signatureDuration(Duration.ofMinutes(
                properties.effectivePresignedUrlTtlMinutes())).getObjectRequest(getObjectRequest).build();

        return s3Presigner.presignGetObject(presignRequest).url().toString();
    }
}
