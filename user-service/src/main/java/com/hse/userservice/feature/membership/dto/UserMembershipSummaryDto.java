package com.hse.userservice.feature.membership.dto;

import java.math.BigDecimal;

public record UserMembershipSummaryDto(
        Long id,
        String coworkingName,
        String status,
        String scheduleLabel,
        String address,
        BigDecimal balance
) {
}
