package com.hse.adminservice.operations.membership.dto;

import java.time.LocalDateTime;
import java.util.List;

public record MembershipProfileResponse(
        Long membershipId,
        Long userId,
        String userName,
        String userEmail,
        String userDescription,
        String status,
        LocalDateTime createdAt,
        LocalDateTime approvedAt,
        LocalDateTime blockedAt,
        Long balanceMinorUnits,
        List<MembershipBookingResponse> activeBookings
) {
}
