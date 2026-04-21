package com.hse.userservice.internal.dto;

import java.time.LocalDate;

public record MembershipQueueItemDto(
        Long membershipId,
        Long userId,
        String userName,
        String coworkingName,
        String status,
        LocalDate createdAt
) {
}
