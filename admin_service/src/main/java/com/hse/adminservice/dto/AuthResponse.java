package com.hse.adminservice.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record AuthResponse(String token, Long adminUserId, List<AccessibleCoworkingResponse> coworkings) {
}
