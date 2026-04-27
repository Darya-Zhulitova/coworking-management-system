package com.hse.adminservice.rbac.authorization;

import com.hse.adminservice.rbac.domain.Grant;
import lombok.Builder;

import java.util.Collections;
import java.util.Set;

@Builder
public record ResolvedAdminAccessContext(
        Long adminId,
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
