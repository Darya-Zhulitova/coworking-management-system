package com.hse.adminservice.space.placetype.dto;

import lombok.Builder;

@Builder
public record PlaceTypeSummaryResponse(
        Long id,
        String name,
        Boolean active,
        Long tariffId,
        String tariffName
) {
}
