package com.hse.adminservice.operations.membership.dto;

import java.time.LocalDateTime;

public record MembershipListItemResponse(
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
