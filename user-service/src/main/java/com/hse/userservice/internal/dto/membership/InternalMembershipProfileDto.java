package com.hse.userservice.internal.dto.membership;

import java.time.LocalDateTime;
import java.util.List;

public record InternalMembershipProfileDto(
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
        List<InternalMembershipBookingDto> activeBookings
) {
}
