package com.hse.userservice.feature.membership.dto;

import com.hse.userservice.feature.user.dto.UserResponse;

public record CoworkingContextDto(
        UserResponse user,
        CoworkingDetailsDto coworking,
        MembershipContextDto membership
) {
}
