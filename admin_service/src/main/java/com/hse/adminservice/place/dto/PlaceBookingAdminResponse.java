package com.hse.adminservice.place.dto;

import lombok.Builder;

import java.time.LocalDate;

@Builder
public record PlaceBookingAdminResponse(
        Long bookingId,
        Long membershipId,
        String userName,
        LocalDate date,
        Long cost,
        Boolean active,
        String status
) {
}
