package com.hse.adminservice.dto;

import com.hse.adminservice.entity.Grant;
import lombok.Builder;

import java.util.Set;

@Builder
public record AppContextResponse(
        Long id,
        String email,
        String name,
        String role,
        Set<Grant> grants,
        Long coworkingId,
        String coworkingName
) {
}
