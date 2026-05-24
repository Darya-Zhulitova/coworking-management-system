package com.hse.userservice.feature.membership.dto;


public record MembershipContextDto(
        Long id,
        String status,
        Long balanceMinorUnits
) {
}
