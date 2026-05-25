package com.hse.adminservice.configlookup.dto;

import lombok.Builder;

@Builder
public record PlaceSummaryResponse(
        Long id,
        String name,
        String floorName,
        String placeTypeName,
        String previewImageUrl,
        String fullImageUrl
) {
}
