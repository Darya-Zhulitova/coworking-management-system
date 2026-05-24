package com.hse.userservice.internal.dto.booking;

import java.time.LocalDate;

public record PlaceBookingAdminDto(
        Long bookingId,
        String bookingNumber,
        Long membershipId,
        String userName,
        LocalDate date,
        Long cost,
        Boolean active,
        String status
) {
}
