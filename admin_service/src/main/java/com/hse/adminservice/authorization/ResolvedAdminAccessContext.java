package com.hse.adminservice.authorization;

import com.hse.adminservice.entity.AdminCoworkingRole;
import com.hse.adminservice.entity.AdminPrincipalType;
import lombok.Builder;

import java.util.Set;

@Builder
public record ResolvedAdminAccessContext(
        AdminPrincipalType principalType,
        Long tenantAdminUserId,
        Long superAdminId,
        Long coworkingId,
        AdminCoworkingRole coworkingRole,
        Set<EffectiveAdminAction> grantedActions
) {
    public boolean hasAction(EffectiveAdminAction action) {
        return grantedActions.contains(action);
    }
}
