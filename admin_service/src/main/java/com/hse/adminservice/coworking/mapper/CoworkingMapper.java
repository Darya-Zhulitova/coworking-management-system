package com.hse.adminservice.coworking.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hse.adminservice.coworking.domain.Coworking;
import com.hse.adminservice.coworking.dto.CoworkingPublicInfoResponse;
import com.hse.adminservice.coworking.dto.CoworkingResponse;
import com.hse.adminservice.files.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CoworkingMapper {
    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;
    private final FileStorageService fileStorageService;

    public CoworkingResponse toResponse(Coworking coworking) {
        List<String> uploadedImageFileIds = readImageFileIds(coworking.getImageFileIdsJson());
        List<String> uploadedImageUrls = uploadedImageFileIds.stream().map(fileStorageService::presignedUrl).filter(
                value -> value != null && !value.isBlank()).toList();
        return CoworkingResponse.builder()
                .id(coworking.getId())
                .name(coworking.getName())
                .description(coworking.getDescription())
                .address(coworking.getAddress())
                .workingHoursLabel(coworking.getWorkingHoursLabel())
                .heroTitle(coworking.getHeroTitle())
                .heroText(coworking.getHeroText())
                .imageUrls(uploadedImageUrls)
                .uploadedImageUrls(uploadedImageUrls)
                .uploadedImageFileIds(uploadedImageFileIds)
                .schedule(coworking.getSchedule())
                .autoApproveMembership(coworking.getAutoApproveMembership())
                .floorMapEnabled(Boolean.TRUE.equals(coworking.getFloorMapEnabled()))
                .joinToken(coworking.getJoinToken())
                .active(coworking.getActive())
                .archived(coworking.getArchived())
                .configurationVersion(coworking.getConfigurationVersion())
                .archivedAt(coworking.getArchivedAt())
                .createdAt(coworking.getCreatedAt())
                .updatedAt(coworking.getUpdatedAt())
                .build();
    }

    public CoworkingPublicInfoResponse toPublicInfoResponse(Coworking coworking) {
        List<String> uploadedImageUrls = resolveImageUrls(coworking.getImageFileIdsJson());
        return CoworkingPublicInfoResponse.builder()
                .id(coworking.getId())
                .name(coworking.getName())
                .description(coworking.getDescription())
                .address(coworking.getAddress())
                .workingHoursLabel(coworking.getWorkingHoursLabel())
                .heroTitle(coworking.getHeroTitle())
                .heroText(coworking.getHeroText())
                .imageUrls(uploadedImageUrls)
                .autoApproveMembership(coworking.getAutoApproveMembership())
                .floorMapEnabled(Boolean.TRUE.equals(coworking.getFloorMapEnabled()))
                .active(coworking.getActive())
                .build();
    }

    public String writeImageUrls(List<String> imageUrls) {
        return writeStringList(List.of(), "Не удалось сохранить ссылки на изображения коворкинга.");
    }

    public String writeImageFileIds(List<String> fileIds) {
        return writeStringList(normalizeImageUrls(fileIds), "Не удалось сохранить изображения коворкинга.");
    }

    public List<String> readImageUrls(String imageUrlsJson) {
        return List.of();
    }

    public List<String> readImageFileIds(String imageFileIdsJson) {
        return readStringList(imageFileIdsJson, "Не удалось прочитать изображения коворкинга.");
    }

    private String writeStringList(List<String> values, String errorMessage) {
        try {
            return objectMapper.writeValueAsString(normalizeImageUrls(values));
        } catch (JsonProcessingException exception) {
            log.error(errorMessage, exception);
            throw new IllegalArgumentException(errorMessage, exception);
        }
    }

    private List<String> readStringList(String json, String errorMessage) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return normalizeImageUrls(objectMapper.readValue(json, STRING_LIST_TYPE));
        } catch (JsonProcessingException exception) {
            log.error(errorMessage, exception);
            throw new IllegalArgumentException(errorMessage, exception);
        }
    }

    private List<String> normalizeImageUrls(List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            return Collections.emptyList();
        }
        return imageUrls.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
    }

    private List<String> resolveImageUrls(String imageFileIdsJson) {
        return readImageFileIds(imageFileIdsJson).stream()
                .map(fileStorageService::presignedUrl)
                .filter(value -> value != null && !value.isBlank())
                .toList();
    }
}
