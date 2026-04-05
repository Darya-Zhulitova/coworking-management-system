package com.hse.adminservice.service;

import com.hse.adminservice.authorization.AdminAuthorizationService;
import com.hse.adminservice.authorization.EffectiveAdminAction;
import com.hse.adminservice.dto.AssignTenantRoleRequest;
import com.hse.adminservice.dto.TenantRoleResponse;
import com.hse.adminservice.dto.TenantStaffMemberResponse;
import com.hse.adminservice.dto.UpdateTenantRoleRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TenantStaffService {

    private final AdminAuthorizationService authorizationService;
    private final AdminCoworkingAccessService adminCoworkingAccessService;

    public List<TenantStaffMemberResponse> getStaff(Long coworkingId) {
        authorizationService.requireCoworkingAction(coworkingId, EffectiveAdminAction.VIEW_STAFF_ACCESS);
        return adminCoworkingAccessService.getTenantStaff(coworkingId);
    }

    public List<TenantRoleResponse> getAvailableRoles(Long coworkingId) {
        authorizationService.requireCoworkingAction(coworkingId, EffectiveAdminAction.VIEW_STAFF_ACCESS);
        return adminCoworkingAccessService.getAvailableRoles();
    }

    public TenantStaffMemberResponse assignRole(Long coworkingId, AssignTenantRoleRequest request) {
        authorizationService.requireCoworkingAction(coworkingId, EffectiveAdminAction.MANAGE_STAFF_ACCESS);
        return adminCoworkingAccessService.assignRole(coworkingId, request.getEmail(), request.getRole());
    }

    public TenantStaffMemberResponse updateRole(Long coworkingId, Long accessId, UpdateTenantRoleRequest request) {
        authorizationService.requireCoworkingAction(coworkingId, EffectiveAdminAction.MANAGE_STAFF_ACCESS);
        return adminCoworkingAccessService.updateRole(coworkingId, accessId, request.getRole(), request.getActive());
    }

    public void deactivate(Long coworkingId, Long accessId) {
        authorizationService.requireCoworkingAction(coworkingId, EffectiveAdminAction.MANAGE_STAFF_ACCESS);
        adminCoworkingAccessService.deactivate(coworkingId, accessId);
    }
}
