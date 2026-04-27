package com.hse.adminservice.operations.bookingimpact.dto;

import lombok.Builder;

@Builder
public record PlaceOperationalResponse(
        Long placeId,
        String placeName,
        String placeTypeName,
        Integer totalBookings,
        Integer unfinishedBookings,
        Boolean active,
        Long floorId
) {
}
