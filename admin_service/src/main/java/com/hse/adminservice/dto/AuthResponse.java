package com.hse.adminservice.dto;

import lombok.Builder;

@Builder
public record AuthResponse(
        String token,
        Long adminId
) {
}
