package com.hse.adminservice.operations.bookingimpact.dto;

import lombok.Builder;

@Builder
public record BookingPlaceResponse(
        Long placeId,
        String placeName,
        Long coworkingId,
        String coworkingName
) {
}
