package com.hse.userservice.internal.dto;

import java.time.LocalDate;

public record PayRequestQueueItemDto(
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
