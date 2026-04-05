package com.hse.adminservice.dto;

import com.hse.adminservice.authorization.EffectiveAdminAction;
import com.hse.adminservice.entity.AdminCoworkingRole;
import lombok.Builder;

import java.util.Set;

@Builder
public record TenantRoleResponse(
        AdminCoworkingRole code,
        String label,
        Set<EffectiveAdminAction> grantedActions
) {
}
