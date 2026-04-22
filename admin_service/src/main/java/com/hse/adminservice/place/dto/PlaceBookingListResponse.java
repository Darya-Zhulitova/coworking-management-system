package com.hse.adminservice.place.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record PlaceBookingListResponse(
        Long coworkingId,
        Long placeId,
        String source,
        String message,
        List<PlaceBookingAdminResponse> bookings
) {
}
