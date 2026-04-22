package com.hse.adminservice.coworking.dto;

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
        Integer cost,
        Long balanceMinorUnits,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime resolvedAt
) {
}
