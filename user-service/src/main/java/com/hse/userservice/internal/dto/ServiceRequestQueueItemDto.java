package com.hse.userservice.internal.dto;

import java.time.LocalDate;

public record ServiceRequestQueueItemDto(
        Long serviceRequestId,
        Long membershipId,
        Long userId,
        String userName,
        String typeName,
        String name,
        Integer cost,
        String status,
        LocalDate createdAt
) {
}
