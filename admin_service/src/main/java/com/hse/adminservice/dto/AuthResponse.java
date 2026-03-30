package com.hse.adminservice.dto;

import com.hse.adminservice.authorization.EffectiveAdminAction;
import com.hse.adminservice.entity.AdminPrincipalType;
import lombok.Builder;

import java.util.List;
import java.util.Set;

@Builder
public record AuthResponse(
        String token,
        Long adminUserId,
        Long superAdminId,
        AdminPrincipalType principalType,
        Set<EffectiveAdminAction> grantedGlobalActions,
        List<AccessibleCoworkingResponse> coworkings
) {
}
