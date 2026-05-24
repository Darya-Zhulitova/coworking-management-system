package com.hse.userservice.feature.booking.dto;

import java.time.LocalDate;
import java.util.List;

public record BookingInitResponseDto(
        String coworkingName,
        Long membershipId,
        String membershipStatus,
        Long balanceMinorUnits,
        LocalDate previewDate,
        Boolean floorMapEnabled,
        List<FloorItemDto> floors,
        List<PlaceItemDto> places
) {
    public record FloorItemDto(
            Long id,
            String name,
            Integer index,
            String imageFileId,
            String imageUrl,
            Boolean active
    ) {
    }

    public record PlaceItemDto(
            Long id,
            String name,
            Long floorId,
            String floorName,
            Long placeTypeId,
            String placeTypeName,
            Long pricePerDay,
            List<String> amenities,
            java.math.BigDecimal locX,
            java.math.BigDecimal locY,
            String imageFileId,
            String previewImageUrl,
            String fullImageUrl,
            Boolean active,
            Boolean available
    ) {
    }
}
