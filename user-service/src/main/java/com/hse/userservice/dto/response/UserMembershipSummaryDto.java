package com.hse.userservice.dto.response;

import java.time.LocalDateTime;

public record UserMembershipSummaryDto(
        Long id,
        Long coworkingId,
        LocalDateTime createdAt
) {
}
