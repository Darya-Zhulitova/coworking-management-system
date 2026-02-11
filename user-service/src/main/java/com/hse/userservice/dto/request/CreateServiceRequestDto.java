package com.hse.userservice.dto.request;

public record CreateServiceRequestDto(
        Long membershipId,
        Long placeId,
        Long bookingId,
        String category,
        String description
) {
}