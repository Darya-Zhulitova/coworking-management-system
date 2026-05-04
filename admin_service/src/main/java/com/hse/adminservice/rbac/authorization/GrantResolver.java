package com.hse.adminservice.rbac.authorization;

import com.hse.adminservice.rbac.domain.Access;
import com.hse.adminservice.rbac.domain.Grant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class GrantResolver {
    private final SystemCoworkingRoleDefinitions roleDefinitions;

    public Set<Grant> resolveGrantedActions(Access access) {
        if (!Boolean.TRUE.equals(access.getActive()) || !Boolean.TRUE.equals(access.getRole().getActive())) {
            return EnumSet.noneOf(Grant.class);
        }
        return normalizeRoleGrants(roleDefinitions.parseGrants(access.getRole().getGrantsRaw()));
    }

    public Set<Grant> resolveOwnerGrantedActions() {
        return EnumSet.allOf(Grant.class);
    }

    public EnumSet<Grant> normalizeRoleGrants(Set<Grant> grants) {
        EnumSet<Grant> normalized = grants == null || grants.isEmpty() ? EnumSet.noneOf(Grant.class) : EnumSet.copyOf(
                grants);
        if (normalized.contains(Grant.COWORKING_EDIT)) {
            normalized.add(Grant.COWORKING_READ);
        }
        if (normalized.contains(Grant.FLOOR_EDIT)) {
            normalized.add(Grant.FLOOR_READ);
        }
        if (normalized.contains(Grant.PLACE_TYPE_EDIT)) {
            normalized.add(Grant.PLACE_TYPE_READ);
        }
        if (normalized.contains(Grant.PLACE_EDIT)) {
            normalized.add(Grant.PLACE_READ);
        }
        if (normalized.contains(Grant.TARIFF_EDIT)) {
            normalized.add(Grant.TARIFF_READ);
        }
        if (normalized.contains(Grant.SERVICE_REQUEST_TYPE_EDIT)) {
            normalized.add(Grant.SERVICE_REQUEST_TYPE_READ);
        }
        if (normalized.contains(Grant.ROLE_EDIT)) {
            normalized.add(Grant.ROLE_READ);
        }
        if (normalized.contains(Grant.ACCESS_EDIT)) {
            normalized.add(Grant.ACCESS_READ);
        }
        if (normalized.contains(Grant.SCHEDULE_EDIT)) {
            normalized.add(Grant.SCHEDULE_READ);
        }
        if (normalized.contains(Grant.USER_EDIT)) {
            normalized.add(Grant.USER_READ);
        }
        if (normalized.contains(Grant.BOOKING_EDIT)) {
            normalized.add(Grant.BOOKING_READ);
        }
        return normalized;
    }
}
