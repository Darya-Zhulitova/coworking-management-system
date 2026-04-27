package com.hse.adminservice.operations.bookingimpact.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
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
