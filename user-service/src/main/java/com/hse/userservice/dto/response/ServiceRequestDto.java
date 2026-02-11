package com.hse.userservice.dto.response;

import com.hse.userservice.domain.request.ServiceRequestStatus;

import java.time.LocalDateTime;

public record ServiceRequestDto(
        Long id,
        Long membershipId,
        Long placeId,
        Long bookingId,
        String category,
        String description,
        ServiceRequestStatus status,
        LocalDateTime createdAt
) {
}