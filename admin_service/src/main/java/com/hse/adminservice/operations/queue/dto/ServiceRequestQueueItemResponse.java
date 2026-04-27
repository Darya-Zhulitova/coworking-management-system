package com.hse.adminservice.operations.queue.dto;

import lombok.Builder;

import java.time.LocalDate;

@Builder
public record ServiceRequestQueueItemResponse(
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
