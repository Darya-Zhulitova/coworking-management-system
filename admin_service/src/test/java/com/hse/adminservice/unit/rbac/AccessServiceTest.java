package com.hse.adminservice.unit.rbac;

import com.hse.adminservice.coworking.domain.Coworking;
import com.hse.adminservice.coworking.dto.CoworkingListItemResponse;
import com.hse.adminservice.rbac.application.AccessService;
import com.hse.adminservice.rbac.application.AccessibleCoworkingQueryService;
import com.hse.adminservice.rbac.application.CoworkingRoleService;
import com.hse.adminservice.rbac.application.CoworkingStaffAccessService;
import com.hse.adminservice.rbac.authorization.GrantResolver;
import com.hse.adminservice.rbac.domain.Access;
import com.hse.adminservice.rbac.domain.Grant;
import com.hse.adminservice.rbac.dto.CoworkingAccessResponse;
import com.hse.adminservice.rbac.dto.CoworkingRoleResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccessServiceTest {

    @Mock private AccessibleCoworkingQueryService queryService;
    @Mock private CoworkingStaffAccessService staffAccessService;
    @Mock private CoworkingRoleService roleService;
    @Mock private GrantResolver grantResolver;

    private AccessService service;

    @BeforeEach
    void setUp() {
        service = new AccessService(queryService, staffAccessService, roleService, grantResolver);
    }

    @Test
    void delegatesAccessibleCoworkingQueries() {
        List<CoworkingListItemResponse> expected = List.of(CoworkingListItemResponse.builder().id(1L).build());
        when(queryService.getAccessibleCoworkings(7L)).thenReturn(expected);

        assertThat(service.getAccessibleCoworkings(7L)).isSameAs(expected);
    }

    @Test
    void delegatesDisplayAccessLabelResolution() {
        Coworking coworking = Coworking.builder().id(1L).build();
        when(queryService.resolveDisplayAccessLabel(7L, coworking)).thenReturn("Owner");

        assertThat(service.resolveDisplayAccessLabel(7L, coworking)).isEqualTo("Owner");
    }

    @Test
    void delegatesStaffAccessCommands() {
        CoworkingAccessResponse response = CoworkingAccessResponse.builder().accessId(1L).build();
        when(staffAccessService.assignRole(10L, "staff@example.test", 5L)).thenReturn(response);
        when(staffAccessService.updateAccessRole(10L, 1L, 5L, true)).thenReturn(response);

        assertThat(service.assignRole(10L, "staff@example.test", 5L)).isSameAs(response);
        assertThat(service.updateAccessRole(10L, 1L, 5L, true)).isSameAs(response);
        service.deactivate(10L, 1L);

        verify(staffAccessService).deactivate(10L, 1L);
    }

    @Test
    void delegatesRoleCommandsAndQueries() {
        CoworkingRoleResponse role = CoworkingRoleResponse.builder().roleId(5L).build();
        Set<Grant> grants = EnumSet.of(Grant.ROLE_EDIT);
        when(roleService.getRole(10L, 5L)).thenReturn(role);
        when(roleService.createRole(10L, "Managers", grants, true)).thenReturn(role);
        when(roleService.updateRoleDefinition(10L, 5L, "Managers", grants, false)).thenReturn(role);

        assertThat(service.getRole(10L, 5L)).isSameAs(role);
        assertThat(service.createRole(10L, "Managers", grants, true)).isSameAs(role);
        assertThat(service.updateRoleDefinition(10L, 5L, "Managers", grants, false)).isSameAs(role);
        service.archiveRole(10L, 5L);

        verify(roleService).archiveRole(10L, 5L);
    }

    @Test
    void delegatesGrantResolution() {
        Access access = Access.builder().build();
        when(grantResolver.resolveGrantedActions(access)).thenReturn(EnumSet.of(Grant.ACCESS_READ));
        when(grantResolver.resolveOwnerGrantedActions()).thenReturn(EnumSet.allOf(Grant.class));

        assertThat(service.resolveGrantedActions(access)).containsExactly(Grant.ACCESS_READ);
        assertThat(service.resolveOwnerGrantedActions()).containsExactlyInAnyOrder(Grant.values());
    }
}
