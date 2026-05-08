package com.hse.adminservice.operations.bookingimpact.dto;

import lombok.Builder;

@Builder
public record BookingUserResponse(
        Long membershipId,
        Long userId,
        String name
) {
}
