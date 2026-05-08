package com.hse.adminservice.rbac.application;

import com.hse.adminservice.rbac.authorization.AdminAuthorizationService;
import com.hse.adminservice.rbac.domain.Grant;
import com.hse.adminservice.rbac.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CoworkingAccessService {

    private final AdminAuthorizationService authorizationService;
    private final AccessService adminCoworkingAccessService;

    public List<CoworkingAccessResponse> getStaff(Long coworkingId) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.ACCESS_READ);
        return adminCoworkingAccessService.getCoworkingAccessList(coworkingId);
    }

    public List<CoworkingRoleResponse> getAvailableRoles(Long coworkingId) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.ROLE_READ);
        return adminCoworkingAccessService.getAvailableRoles(coworkingId);
    }

    public List<CoworkingRoleResponse> getAllRoles(Long coworkingId) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.ROLE_READ);
        return adminCoworkingAccessService.getAllRoles(coworkingId);
    }

    public CoworkingRoleResponse getRole(Long coworkingId, Long roleId) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.ROLE_READ);
        return adminCoworkingAccessService.getRole(coworkingId, roleId);
    }

    public CoworkingRoleResponse createRole(Long coworkingId, RoleCreateRequest request) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.ROLE_EDIT);
        return adminCoworkingAccessService.createRole(
                coworkingId,
                request.getName(),
                request.getGrants(),
                request.getActive()
        );
    }

    public CoworkingRoleResponse updateRole(Long coworkingId, Long roleId, RoleUpdateRequest request) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.ROLE_EDIT);
        return adminCoworkingAccessService.updateRoleDefinition(
                coworkingId,
                roleId,
                request.getName(),
                request.getGrants(),
                request.getActive()
        );
    }

    public void archiveRole(Long coworkingId, Long roleId) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.ROLE_EDIT);
        adminCoworkingAccessService.archiveRole(coworkingId, roleId);
    }

    public CoworkingAccessResponse assignRole(Long coworkingId, AssignCoworkingRoleRequest request) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.ACCESS_EDIT);
        return adminCoworkingAccessService.assignRole(coworkingId, request.email(), request.roleId());
    }

    public CoworkingAccessResponse updateAssignedRole(
            Long coworkingId,
            Long accessId,
            UpdateCoworkingRoleRequest request
    ) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.ACCESS_EDIT);
        return adminCoworkingAccessService.updateAccessRole(
                coworkingId,
                accessId,
                request.getRoleId(),
                request.getActive()
        );
    }

    public void deactivate(Long coworkingId, Long accessId) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.ACCESS_EDIT);
        adminCoworkingAccessService.deactivate(coworkingId, accessId);
    }
}
