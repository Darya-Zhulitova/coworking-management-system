package com.hse.adminservice.dto;

import com.hse.adminservice.entity.Grant;
import lombok.Builder;

import java.util.Set;

@Builder
public record CoworkingRoleResponse(
        Long roleId,
        String name,
        Set<Grant> grants
) {
}
