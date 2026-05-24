package com.hse.userservice.feature.user.dto;


public record UserResponse(
        Long id,
        String email,
        String name,
        String description
) {
}
