package com.hse.userservice.feature.membership.dto;

public record JoinCoworkingResultDto(
        Long membershipId,
        Long coworkingId,
        String status,
        boolean existingMembership
) {
}
