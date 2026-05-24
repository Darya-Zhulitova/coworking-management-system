package com.hse.userservice.internal.dto.membership;

import java.time.LocalDateTime;

public record InternalMembershipListItemDto(
        Long membershipId,
        Long userId,
        String userName,
        String userEmail,
        String status,
        LocalDateTime createdAt,
        LocalDateTime approvedAt,
        LocalDateTime blockedAt,
        Long balanceMinorUnits,
        Integer activeBookingsCount
) {
}
