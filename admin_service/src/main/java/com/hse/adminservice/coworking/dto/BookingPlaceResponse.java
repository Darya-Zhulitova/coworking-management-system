package com.hse.adminservice.coworking.dto;

import lombok.Builder;

@Builder
public record BookingPlaceResponse(
        Long placeId,
        String placeName,
        Long coworkingId,
        String coworkingName
) {
}
