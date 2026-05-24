package com.hse.adminservice.unit.rbac;

import com.hse.adminservice.rbac.authorization.GrantResolver;
import com.hse.adminservice.rbac.authorization.SystemCoworkingRoleDefinitions;
import com.hse.adminservice.rbac.domain.Access;
import com.hse.adminservice.rbac.domain.Grant;
import com.hse.adminservice.rbac.domain.Role;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class GrantResolverTest {

    private final SystemCoworkingRoleDefinitions roleDefinitions = new SystemCoworkingRoleDefinitions();
    private final GrantResolver grantResolver = new GrantResolver(roleDefinitions);

    @Test
    void editGrantsAutomaticallyIncludeReadGrants() {
        Set<Grant> normalized = grantResolver.normalizeRoleGrants(EnumSet.of(
                Grant.COWORKING_EDIT,
                Grant.FLOOR_EDIT,
                Grant.PLACE_TYPE_EDIT,
                Grant.PLACE_EDIT,
                Grant.TARIFF_EDIT,
                Grant.SERVICE_REQUEST_TYPE_EDIT,
                Grant.ROLE_EDIT,
                Grant.ACCESS_EDIT,
                Grant.SCHEDULE_EDIT,
                Grant.USER_EDIT,
                Grant.BOOKING_EDIT
        ));

        assertThat(normalized).contains(
                Grant.COWORKING_READ,
                Grant.FLOOR_READ,
                Grant.PLACE_TYPE_READ,
                Grant.PLACE_READ,
                Grant.TARIFF_READ,
                Grant.SERVICE_REQUEST_TYPE_READ,
                Grant.ROLE_READ,
                Grant.ACCESS_READ,
                Grant.SCHEDULE_READ,
                Grant.USER_READ,
                Grant.BOOKING_READ
        );
    }

    @Test
    void nullAndEmptyGrantSetsNormalizeToEmptyEnumSet() {
        assertThat(grantResolver.normalizeRoleGrants(null)).isEmpty();
        assertThat(grantResolver.normalizeRoleGrants(Set.of())).isEmpty();
    }

    @Test
    void inactiveAccessDoesNotGrantAnyActions() {
        Role activeRole = Role.builder()
                .active(true)
                .grantsRaw(roleDefinitions.serialize(EnumSet.of(Grant.COWORKING_EDIT)))
                .build();
        Access inactiveAccess = Access.builder().active(false).role(activeRole).build();

        assertThat(grantResolver.resolveGrantedActions(inactiveAccess)).isEmpty();
    }

    @Test
    void inactiveRoleDoesNotGrantAnyActions() {
        Role inactiveRole = Role.builder()
                .active(false)
                .grantsRaw(roleDefinitions.serialize(EnumSet.of(Grant.COWORKING_EDIT)))
                .build();
        Access access = Access.builder().active(true).role(inactiveRole).build();

        assertThat(grantResolver.resolveGrantedActions(access)).isEmpty();
    }

    @Test
    void activeAccessResolvesSerializedRoleGrantsWithReadImplications() {
        Role role = Role.builder()
                .active(true)
                .grantsRaw(roleDefinitions.serialize(EnumSet.of(Grant.TARIFF_EDIT)))
                .build();
        Access access = Access.builder().active(true).role(role).build();

        assertThat(grantResolver.resolveGrantedActions(access)).containsExactlyInAnyOrder(
                Grant.TARIFF_EDIT,
                Grant.TARIFF_READ
        );
    }

    @Test
    void ownerReceivesAllAvailableActions() {
        assertThat(grantResolver.resolveOwnerGrantedActions()).containsExactlyInAnyOrder(Grant.values());
    }
}
