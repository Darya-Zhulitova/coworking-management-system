package com.hse.userservice.internal.dto.deactivation;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AffectedBookingResponse(
        Long bookingId,
        String bookingNumber,
        Long membershipId,
        Long userId,
        String userName,
        Long placeId,
        String placeName,
        LocalDate date,
        BigDecimal bookingAmount,
        BigDecimal compensationAmount
) {
}
