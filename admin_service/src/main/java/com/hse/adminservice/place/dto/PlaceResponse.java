package com.hse.adminservice.place.dto;

import com.hse.adminservice.placetype.dto.PlaceTypeSummaryResponse;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Builder
public record PlaceResponse(
        Long id,
        String name,
        Boolean active,
        Boolean archived,
        LocalDateTime archivedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Long coworkingId,
        Long floorId,
        String floorName,
        BigDecimal locX,
        BigDecimal locY,
        List<String> amenities,
        PlaceTypeSummaryResponse placeType
) {
}
