package com.hse.adminservice.operations.booking.dto;

import lombok.Builder;

import java.time.LocalDate;

@Builder
public record PlaceBookingAdminResponse(
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
