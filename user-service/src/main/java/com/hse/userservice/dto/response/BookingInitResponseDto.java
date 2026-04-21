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
        List<FloorItemDto> floors,
        List<PlaceTypeItemDto> placeTypes,
        List<TariffItemDto> tariffs,
        List<PlaceItemDto> places
) {
    public record FloorItemDto(
            Long id,
            String name,
            Integer index
    ) {
    }

    public record PlaceTypeItemDto(
            Long id,
            String name,
            Long tariffId
    ) {
    }

    public record TariffItemDto(
            Long id,
            String name,
            Integer pricePerDay,
            Integer minBookingDays,
            List<DiscountRuleItemDto> discountRules
    ) {
    }

    public record DiscountRuleItemDto(
            Long id,
            Integer thresholdQuantity,
            Integer discountPercent
    ) {
    }

    public record PlaceItemDto(
            Long id,
            String name,
            Long floorId,
            String floorName,
            Long placeTypeId,
            String placeTypeName,
            Long tariffId,
            Integer pricePerDay,
            List<String> amenities,
            Boolean active,
            Boolean previewAvailable
    ) {
    }
}
