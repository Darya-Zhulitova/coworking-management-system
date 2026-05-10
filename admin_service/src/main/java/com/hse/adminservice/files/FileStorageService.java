package com.hse.adminservice.files;

import com.hse.adminservice.common.error.ConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.InputStream;
import java.time.Duration;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileStorageService {
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final long MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024;

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final FileStorageProperties properties;

    public StoredFileResponse uploadFloorPlan(Long floorId, MultipartFile file) {
        validateImage(file);
        String fileId = createFloorPlanFileId(floorId, file.getContentType());

        try (InputStream inputStream = file.getInputStream()) {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(properties.bucket())
                            .key(fileId)
                            .contentType(file.getContentType())
                            .build(), RequestBody.fromInputStream(inputStream, file.getSize())
            );

            return new StoredFileResponse(fileId, presignedUrl(fileId));
        } catch (Exception exception) {
            throw new ConflictException("Failed to upload floor plan image");
        }
    }

    public String presignedUrl(String fileId) {
        if (!StringUtils.hasText(fileId)) {
            return null;
        }

        GetObjectRequest getObjectRequest = GetObjectRequest.builder().bucket(properties.bucket()).key(fileId).build();
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder().signatureDuration(Duration.ofMinutes(
                properties.effectivePresignedUrlTtlMinutes())).getObjectRequest(getObjectRequest).build();

        return s3Presigner.presignGetObject(presignRequest).url().toString();
    }

    private String createFloorPlanFileId(Long floorId, String contentType) {
        return "floor-plans/%d/%s.%s".formatted(floorId, UUID.randomUUID(), extensionFor(contentType));
    }

    private void validateImage(MultipartFile file) {
        if (file.isEmpty()) {
            throw new ConflictException("File must not be empty");
        }

        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new ConflictException("Image size must not exceed 5 MB");
        }

        String contentType = file.getContentType();

        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new ConflictException("Only JPG, PNG and WEBP images are allowed");
        }
    }

    private String extensionFor(String contentType) {
        return switch (contentType.toLowerCase(Locale.ROOT)) {
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            default -> throw new ConflictException("Unsupported image format");
        };
    }
}
