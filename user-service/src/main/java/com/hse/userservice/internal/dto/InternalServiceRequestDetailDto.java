package com.hse.userservice.internal.dto;

import java.time.LocalDateTime;

public record InternalServiceRequestDetailDto(
        Long serviceRequestId,
        Long membershipId,
        Long userId,
        String userName,
        String userEmail,
        String typeName,
        String name,
        Integer cost,
        Long balanceMinorUnits,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime resolvedAt
) {
}
