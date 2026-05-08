package com.hse.adminservice.operations.queue.dto;

import lombok.Builder;

import java.time.LocalDate;

@Builder
public record MembershipQueueItemResponse(
        Long membershipId,
        Long userId,
        String userName,
        String coworkingName,
        String status,
        LocalDate createdAt
) {
}
