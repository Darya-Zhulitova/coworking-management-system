package com.hse.userservice.internal.dto.membership;

import java.time.LocalDate;

public record InternalMembershipBookingDto(
        Long bookingId,
        String bookingNumber,
        Long placeId,
        LocalDate date,
        Long cost,
        String status
) {
}
