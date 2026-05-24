package com.hse.userservice.integration.dto;

public record PlaceSummary(
        Long id,
        String name,
        String floorName,
        String placeTypeName,
        String previewImageUrl,
        String fullImageUrl
) {
}
