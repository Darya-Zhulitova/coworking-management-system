package com.hse.adminservice.coworking.dto;

import lombok.Builder;

@Builder
public record BookingUserResponse(
        Long membershipId,
        Long userId,
        String name
) {
}
