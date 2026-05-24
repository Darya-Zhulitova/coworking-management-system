package com.hse.adminservice.operations.bookingimpact.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Builder
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
