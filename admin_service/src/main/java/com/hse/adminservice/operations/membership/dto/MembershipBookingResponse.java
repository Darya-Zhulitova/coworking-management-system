package com.hse.adminservice.operations.membership.dto;

import java.time.LocalDate;

public record MembershipBookingResponse(
        Long bookingId,
        String bookingNumber,
        Long placeId,
        String placeName,
        LocalDate date,
        Long cost,
        String status
) {
}
