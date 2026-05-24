package com.hse.userservice.feature.user.dto;


public record AuthResponse(
        String token,
        Long userId
) {
}
