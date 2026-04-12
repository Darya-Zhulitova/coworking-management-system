package com.hse.userservice.dto.response;

import com.hse.userservice.domain.membership.MembershipStatus;

import java.time.LocalDateTime;

public record MembershipDto(
        Long id,
        Long userId,
        Long coworkingId,
        MembershipStatus status,
        LocalDateTime createdAt,
        LocalDateTime approvedAt,
        LocalDateTime blockedAt
) {
}
