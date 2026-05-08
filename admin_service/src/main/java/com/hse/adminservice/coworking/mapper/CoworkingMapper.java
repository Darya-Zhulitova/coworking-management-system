package com.hse.adminservice.coworking.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hse.adminservice.coworking.domain.Coworking;
import com.hse.adminservice.coworking.dto.CoworkingPublicInfoResponse;
import com.hse.adminservice.coworking.dto.CoworkingResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class CoworkingMapper {
    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;

    public CoworkingResponse toResponse(Coworking coworking) {
        return CoworkingResponse.builder()
                .id(coworking.getId())
                .name(coworking.getName())
                .description(coworking.getDescription())
                .address(coworking.getAddress())
                .workingHoursLabel(coworking.getWorkingHoursLabel())
                .heroTitle(coworking.getHeroTitle())
                .heroText(coworking.getHeroText())
                .imageUrls(readImageUrls(coworking.getImageUrlsJson()))
                .schedule(coworking.getSchedule())
                .autoApproveMembership(coworking.getAutoApproveMembership())
                .active(coworking.getActive())
                .archived(coworking.getArchived())
                .configurationVersion(coworking.getConfigurationVersion())
                .archivedAt(coworking.getArchivedAt())
                .createdAt(coworking.getCreatedAt())
                .updatedAt(coworking.getUpdatedAt())
                .build();
    }

    public CoworkingPublicInfoResponse toPublicInfoResponse(Coworking coworking) {
        return CoworkingPublicInfoResponse.builder()
                .id(coworking.getId())
                .name(coworking.getName())
                .description(coworking.getDescription())
                .address(coworking.getAddress())
                .workingHoursLabel(coworking.getWorkingHoursLabel())
                .heroTitle(coworking.getHeroTitle())
                .heroText(coworking.getHeroText())
                .imageUrls(readImageUrls(coworking.getImageUrlsJson()))
                .autoApproveMembership(coworking.getAutoApproveMembership())
                .active(coworking.getActive())
                .build();
    }

    public String writeImageUrls(List<String> imageUrls) {
        try {
            return objectMapper.writeValueAsString(normalizeImageUrls(imageUrls));
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Failed to serialize coworking image URLs.", exception);
        }
    }

    public List<String> readImageUrls(String imageUrlsJson) {
        if (imageUrlsJson == null || imageUrlsJson.isBlank()) {
            return List.of();
        }
        try {
            return normalizeImageUrls(objectMapper.readValue(imageUrlsJson, STRING_LIST_TYPE));
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Failed to parse coworking image URLs.", exception);
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
}
