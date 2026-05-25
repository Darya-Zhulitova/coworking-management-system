package com.hse.adminservice.unit.rbac;

import com.hse.adminservice.rbac.application.AccessService;
import com.hse.adminservice.rbac.application.CoworkingAccessService;
import com.hse.adminservice.rbac.authorization.AdminAuthorizationService;
import com.hse.adminservice.rbac.domain.Grant;
import com.hse.adminservice.rbac.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.EnumSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CoworkingAccessServiceTest {

    @Mock private AdminAuthorizationService authorizationService;
    @Mock private AccessService accessService;

    private CoworkingAccessService service;

    @BeforeEach
    void setUp() {
        service = new CoworkingAccessService(authorizationService, accessService);
    }

    @Test
    void getStaffRequiresAccessReadBeforeDelegating() {
        List<CoworkingAccessResponse> expected = List.of(CoworkingAccessResponse.builder().accessId(1L).build());
        when(accessService.getCoworkingAccessList(10L)).thenReturn(expected);

        List<CoworkingAccessResponse> result = service.getStaff(10L);

        assertThat(result).isSameAs(expected);
        InOrder inOrder = inOrder(authorizationService, accessService);
        inOrder.verify(authorizationService).requireCoworkingAction(10L, Grant.ACCESS_READ);
        inOrder.verify(accessService).getCoworkingAccessList(10L);
    }

    @Test
    void createRoleRequiresRoleEditAndDelegatesRequestFields() {
        RoleCreateRequest request = new RoleCreateRequest();
        request.setName("Managers");
        request.setGrants(EnumSet.of(Grant.TARIFF_EDIT));
        request.setActive(true);
        CoworkingRoleResponse expected = CoworkingRoleResponse.builder().roleId(1L).name("Managers").build();
        when(accessService.createRole(10L, "Managers", EnumSet.of(Grant.TARIFF_EDIT), true)).thenReturn(expected);

        CoworkingRoleResponse result = service.createRole(10L, request);

        assertThat(result).isSameAs(expected);
        InOrder inOrder = inOrder(authorizationService, accessService);
        inOrder.verify(authorizationService).requireCoworkingAction(10L, Grant.ROLE_EDIT);
        inOrder.verify(accessService).createRole(10L, "Managers", EnumSet.of(Grant.TARIFF_EDIT), true);
    }

    @Test
    void readRoleOperationsRequireRoleRead() {
        when(accessService.getAllRoles(10L)).thenReturn(List.of());
        when(accessService.getAvailableRoles(10L)).thenReturn(List.of());
        CoworkingRoleResponse role = CoworkingRoleResponse.builder().roleId(5L).build();
        when(accessService.getRole(10L, 5L)).thenReturn(role);

        assertThat(service.getAllRoles(10L)).isEmpty();
        assertThat(service.getAvailableRoles(10L)).isEmpty();
        assertThat(service.getRole(10L, 5L)).isSameAs(role);

        verify(authorizationService, times(3)).requireCoworkingAction(10L, Grant.ROLE_READ);
    }

    @Test
    void updateRoleRequiresRoleEditAndDelegatesRequestFields() {
        RoleUpdateRequest request = new RoleUpdateRequest();
        request.setName("Updated");
        request.setGrants(EnumSet.of(Grant.ACCESS_EDIT));
        request.setActive(false);
        CoworkingRoleResponse expected = CoworkingRoleResponse.builder().roleId(5L).build();
        when(accessService.updateRoleDefinition(10L, 5L, "Updated", EnumSet.of(Grant.ACCESS_EDIT), false))
                .thenReturn(expected);

        CoworkingRoleResponse result = service.updateRole(10L, 5L, request);

        assertThat(result).isSameAs(expected);
        InOrder inOrder = inOrder(authorizationService, accessService);
        inOrder.verify(authorizationService).requireCoworkingAction(10L, Grant.ROLE_EDIT);
        inOrder.verify(accessService).updateRoleDefinition(10L, 5L, "Updated", EnumSet.of(Grant.ACCESS_EDIT), false);
    }

    @Test
    void archiveRoleRequiresRoleEdit() {
        service.archiveRole(10L, 5L);

        InOrder inOrder = inOrder(authorizationService, accessService);
        inOrder.verify(authorizationService).requireCoworkingAction(10L, Grant.ROLE_EDIT);
        inOrder.verify(accessService).archiveRole(10L, 5L);
    }

    @Test
    void assignRoleRequiresAccessEditAndDelegatesRecordFields() {
        AssignCoworkingRoleRequest request = new AssignCoworkingRoleRequest("staff@example.test", 8L);
        CoworkingAccessResponse expected = CoworkingAccessResponse.builder().accessId(9L).build();
        when(accessService.assignRole(10L, "staff@example.test", 8L)).thenReturn(expected);

        CoworkingAccessResponse result = service.assignRole(10L, request);

        assertThat(result).isSameAs(expected);
        InOrder inOrder = inOrder(authorizationService, accessService);
        inOrder.verify(authorizationService).requireCoworkingAction(10L, Grant.ACCESS_EDIT);
        inOrder.verify(accessService).assignRole(10L, "staff@example.test", 8L);
    }

    @Test
    void updateAssignedRoleRequiresAccessEditAndDelegatesRequestFields() {
        UpdateCoworkingRoleRequest request = new UpdateCoworkingRoleRequest();
        request.setRoleId(8L);
        request.setActive(false);
        CoworkingAccessResponse expected = CoworkingAccessResponse.builder().accessId(9L).build();
        when(accessService.updateAccessRole(10L, 9L, 8L, false)).thenReturn(expected);

        CoworkingAccessResponse result = service.updateAssignedRole(10L, 9L, request);

        assertThat(result).isSameAs(expected);
        InOrder inOrder = inOrder(authorizationService, accessService);
        inOrder.verify(authorizationService).requireCoworkingAction(10L, Grant.ACCESS_EDIT);
        inOrder.verify(accessService).updateAccessRole(10L, 9L, 8L, false);
    }

    @Test
    void deactivateRequiresAccessEdit() {
        service.deactivate(10L, 9L);

        InOrder inOrder = inOrder(authorizationService, accessService);
        inOrder.verify(authorizationService).requireCoworkingAction(10L, Grant.ACCESS_EDIT);
        inOrder.verify(accessService).deactivate(10L, 9L);
    }
}
