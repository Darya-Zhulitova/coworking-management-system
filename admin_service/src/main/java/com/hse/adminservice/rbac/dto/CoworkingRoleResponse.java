package com.hse.adminservice.rbac.dto;

import com.hse.adminservice.rbac.domain.Grant;
import lombok.Builder;

import java.util.Set;

@Builder
public record CoworkingRoleResponse(
        Long roleId,
        String name,
        Set<Grant> grants,
        Boolean active
) {
}
