package com.hse.userservice.internal.dto.deactivation;


public record BookingUserResponse(
        Long membershipId,
        Long userId,
        String name,
        String email
) {
}
