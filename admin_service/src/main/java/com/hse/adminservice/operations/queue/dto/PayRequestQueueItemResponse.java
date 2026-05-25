package com.hse.adminservice.operations.queue.dto;

import lombok.Builder;

import java.time.LocalDate;

@Builder
public record PayRequestQueueItemResponse(
        Long payRequestId,
        Long membershipId,
        Long userId,
        String userName,
        Long amount,
        String status,
        String userComment,
        String adminComment,
        LocalDate createdAt
) {
}
