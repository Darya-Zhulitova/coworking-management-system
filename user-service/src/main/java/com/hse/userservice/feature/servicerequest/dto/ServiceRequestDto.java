package com.hse.userservice.feature.servicerequest.dto;

import com.hse.userservice.feature.servicerequest.domain.ServiceRequestStatus;

import java.time.LocalDateTime;

public record ServiceRequestDto(
        Long id,
        Long membershipId,
        Long typeId,
        String name,
        String typeName,
        Long cost,
        ServiceRequestStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime resolvedAt
) {
}
