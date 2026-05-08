package com.hse.adminservice.auth.dto;

public record AuthResponse(
        String token,
        Long adminId
) {
}
