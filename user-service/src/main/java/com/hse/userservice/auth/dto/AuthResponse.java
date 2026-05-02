package com.hse.userservice.auth.dto;

public record AuthResponse(
        String token,
        Long userId
) {
}
