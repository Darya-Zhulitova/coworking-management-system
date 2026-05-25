package com.hse.adminservice.operations.servicedesk.dto;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record ServiceRequestDetailResponse(
        Long serviceRequestId,
        Long membershipId,
        Long userId,
        String userName,
        String userEmail,
        String typeName,
        String name,
        Long cost,
        Long balanceMinorUnits,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime resolvedAt
) {
}
