package com.hse.userservice.dto.response;

import java.time.LocalDate;
import java.util.List;

public record BookingInitResponseDto(
        Long coworkingId,
        String coworkingName,
        Long membershipId,
        String membershipStatus,
        Long balanceMinorUnits,
        LocalDate previewDate,
        List<PlaceItemDto> places
) {
    public record PlaceItemDto(
            Long id,
            String name,
            String floorName,
            String placeTypeName,
            Long tariffId,
            Integer pricePerDay,
            List<String> amenities,
            Boolean active,
            Boolean previewAvailable
    ) {
    }
}
