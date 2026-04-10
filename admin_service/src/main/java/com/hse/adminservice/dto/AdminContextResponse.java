package com.hse.adminservice.dto;

import lombok.Builder;

@Builder
public record AdminContextResponse(
        Long id,
        String email,
        String name
) {
}
