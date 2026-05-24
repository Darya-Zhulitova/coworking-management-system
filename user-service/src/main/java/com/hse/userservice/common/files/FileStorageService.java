package com.hse.userservice.common.files;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageService {
    private static final long MAX_FILE_SIZE_BYTES = 50 * 1024 * 1024;

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final FileStorageProperties properties;

    public StoredFileResponse uploadServiceRequestAttachment(
            Long serviceRequestId,
            Long messageId,
            MultipartFile file
    ) {
        validateAttachment(file);
        String fileId = createAttachmentFileId(serviceRequestId, messageId, file);
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
            log.error("Failed to upload service request attachment", exception);
            throw new IllegalStateException("Не удалось загрузить вложение к сервисной заявке", exception);
        }
    }

    public String presignedUrl(String fileId) {
        if (!StringUtils.hasText(fileId))
            return null;
        GetObjectRequest getObjectRequest = GetObjectRequest.builder().bucket(properties.bucket()).key(fileId).build();
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder().signatureDuration(Duration.ofMinutes(
                properties.effectivePresignedUrlTtlMinutes())).getObjectRequest(getObjectRequest).build();
        return s3Presigner.presignGetObject(presignRequest).url().toString();
    }

    private String createAttachmentFileId(Long serviceRequestId, Long messageId, MultipartFile file) {
        String extension = extensionFor(file);
        return "service-requests/%d/messages/%d/%s.%s".formatted(
                serviceRequestId,
                messageId,
                UUID.randomUUID(),
                extension
        );
    }

    private void validateAttachment(MultipartFile file) {
        if (file == null || file.isEmpty())
            throw new IllegalArgumentException("Файл не должен быть пустым");
        if (file.getSize() > MAX_FILE_SIZE_BYTES)
            throw new IllegalArgumentException("Размер файла не должен превышать 50 МБ");

    }

    private String extensionFor(MultipartFile file) {
        String originalName = file.getOriginalFilename();
        if (StringUtils.hasText(originalName) && originalName.contains(".")) {
            String extension = originalName.substring(originalName.lastIndexOf('.') + 1)
                    .replaceAll("[^A-Za-z0-9]", "")
                    .toLowerCase(Locale.ROOT);
            if (StringUtils.hasText(extension) && extension.length() <= 12) {
                return extension;
            }
        }

        String contentType = file.getContentType();
        if (!StringUtils.hasText(contentType)) {
            return "bin";
        }

        return switch (contentType.toLowerCase(Locale.ROOT)) {
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            case "application/pdf" -> "pdf";
            case "text/plain" -> "txt";
            default -> "bin";
        };
    }
}
