package com.hse.adminservice.authorization;

import com.hse.adminservice.entity.AdminPrincipalType;
import com.hse.adminservice.entity.Grant;
import lombok.Builder;

import java.util.Collections;
import java.util.Set;

@Builder
public record ResolvedAdminAccessContext(
        AdminPrincipalType principalType,
        Long coworkingAdminId,
        Long coworkingId,
        boolean owner,
        Set<Grant> grants
) {
    public ResolvedAdminAccessContext {
        grants = grants == null ? Collections.emptySet() : Set.copyOf(grants);
    }

    public boolean hasAction(Grant action) {
        return grants.contains(action);
    }
}
