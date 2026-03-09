package com.hse.userservice.dto.response;

import com.hse.userservice.domain.membership.MembershipStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MembershipDto(
        Long id, // TODO userId
        Long coworkingId,
        BigDecimal balance,
        MembershipStatus status,
        LocalDateTime createdAt
) {
}