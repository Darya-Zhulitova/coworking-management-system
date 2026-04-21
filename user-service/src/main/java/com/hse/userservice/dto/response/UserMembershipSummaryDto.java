package com.hse.userservice.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record UserMembershipSummaryDto(
        Long id,
        Long coworkingId,
        String coworkingName,
        String status,
        String scheduleLabel,
        String address,
        BigDecimal balance,
        LocalDateTime createdAt
) {
}
