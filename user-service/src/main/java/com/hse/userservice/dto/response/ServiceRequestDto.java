package com.hse.userservice.dto.response;

import com.hse.userservice.domain.request.ServiceRequestStatus;

import java.time.LocalDateTime;

public record ServiceRequestDto(
        Long id,
        Long membershipId,
        Long typeId,
        Long coworkingId,
        String name,
        String typeName,
        Integer cost,
        ServiceRequestStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime resolvedAt
) {
}
