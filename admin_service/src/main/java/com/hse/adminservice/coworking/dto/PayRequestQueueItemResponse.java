package com.hse.adminservice.coworking.dto;

import lombok.Builder;

import java.time.LocalDate;

@Builder
public record PayRequestQueueItemResponse(
        Long payRequestId,
        Long membershipId,
        Long userId,
        String userName,
        Integer amount,
        String status,
        String userComment,
        String adminComment,
        LocalDate createdAt
) {
}
