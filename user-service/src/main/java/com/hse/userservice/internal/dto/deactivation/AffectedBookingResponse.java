package com.hse.userservice.internal.dto.deactivation;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AffectedBookingResponse(
        Long bookingId,
        String status,
        LocalDateTime startAt,
        LocalDateTime endAt,
        BigDecimal bookingAmount,
        BigDecimal compensationAmount,
        BookingUserResponse user,
        BookingPlaceResponse place
) {
}
