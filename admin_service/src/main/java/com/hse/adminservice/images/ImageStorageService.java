package com.hse.adminservice.images;

import com.hse.adminservice.common.error.ConflictException;
import com.hse.adminservice.files.FileStorageProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageStorageService {
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final long MAX_IMAGE_SIZE_BYTES = 10L * 1024 * 1024;
    private static final String IMAGE_CONTENT_TYPE = "image/jpeg";

    private final S3Client s3Client;
    private final FileStorageProperties properties;
    private final ImageProcessingService imageProcessingService;

    public ImageFileKeys uploadFloorPlan(Long floorId, MultipartFile file) {
        byte[] sourceBytes = validateAndReadImage(file);
        ImageProcessingResult image = imageProcessingService.processFloorPlan(sourceBytes);
        String basePath = "images/floors/%d/%s".formatted(floorId, UUID.randomUUID());
        return uploadFullOnly(basePath, image.full(), "Не удалось загрузить план этажа");
    }

    public ImageFileKeys uploadCoworkingPhoto(Long coworkingId, MultipartFile file) {
        byte[] sourceBytes = validateAndReadImage(file);
        ImageProcessingResult image = imageProcessingService.processCoworkingPhoto(sourceBytes);
        String basePath = "images/coworkings/%d/%s".formatted(coworkingId, UUID.randomUUID());
        return uploadFullOnly(basePath, image.full(), "Не удалось загрузить фотографию коворкинга");
    }

    public ImageFileKeys uploadPlacePhoto(Long placeId, MultipartFile file) {
        byte[] sourceBytes = validateAndReadImage(file);
        ImageProcessingResult image = imageProcessingService.processPlacePhoto(sourceBytes);
        String basePath = "images/places/%d/%s".formatted(placeId, UUID.randomUUID());
        ImageFileKeys keys = new ImageFileKeys(basePath + "/full.jpg", basePath + "/preview.jpg");
        try {
            putObject(keys.fullKey(), image.full());
            putObject(keys.previewKey(), image.preview());
            return keys;
        } catch (Exception exception) {
            log.error("Failed to upload place image variants", exception);
            throw new ConflictException("Не удалось загрузить фотографию места");
        }
    }

    private ImageFileKeys uploadFullOnly(String basePath, byte[] fullContent, String errorMessage) {
        ImageFileKeys keys = new ImageFileKeys(basePath + "/full.jpg", null);
        try {
            putObject(keys.fullKey(), fullContent);
            return keys;
        } catch (Exception exception) {
            log.error("Failed to upload image", exception);
            throw new ConflictException(errorMessage);
        }
    }

    private byte[] validateAndReadImage(MultipartFile file) {
        validateImage(file);
        try {
            return file.getBytes();
        } catch (IOException exception) {
            log.error("Failed to read uploaded image bytes", exception);
            throw new ConflictException("Не удалось обработать изображение. Загрузите другой файл.");
        }
    }

    private void putObject(String key, byte[] content) {
        s3Client.putObject(
                PutObjectRequest.builder().bucket(properties.bucket()).key(key).contentType(IMAGE_CONTENT_TYPE).build(),
                RequestBody.fromBytes(content)
        );
    }

    private void validateImage(MultipartFile file) {
        if (file.isEmpty()) {
            throw new ConflictException("Файл не должен быть пустым");
        }
        if (file.getSize() > MAX_IMAGE_SIZE_BYTES) {
            throw new ConflictException("Размер изображения не должен превышать 10 МБ.");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new ConflictException("Загрузите изображение JPG, PNG или WEBP.");
        }
    }
}
