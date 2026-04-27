package com.hse.adminservice.space.placetype.dto;

import com.hse.adminservice.pricing.tariff.dto.TariffResponse;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record PlaceTypeResponse(
        Long id,
        Long coworkingId,
        String name,
        Boolean active,
        Boolean archived,
        LocalDateTime archivedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        TariffResponse tariff
) {
}
