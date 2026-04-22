package com.hse.adminservice.context.dto;

import com.hse.adminservice.rbac.entity.Grant;
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
