package com.hse.userservice.internal.dto.booking;

import java.util.List;

public record PlaceBookingListDto(
        Long coworkingId,
        Long placeId,
        String source,
        String message,
        List<PlaceBookingAdminDto> bookings
) {
}
