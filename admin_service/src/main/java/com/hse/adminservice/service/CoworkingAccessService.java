package com.hse.adminservice.service;

import com.hse.adminservice.authorization.AdminAuthorizationService;
import com.hse.adminservice.entity.Grant;
import com.hse.adminservice.dto.AssignCoworkingRoleRequest;
import com.hse.adminservice.dto.CoworkingRoleResponse;
import com.hse.adminservice.dto.CoworkingAccessResponse;
import com.hse.adminservice.dto.UpdateCoworkingRoleRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CoworkingAccessService {

    private final AdminAuthorizationService authorizationService;
    private final AccessService adminCoworkingAccessService;

    public List<CoworkingAccessResponse> getStaff(Long coworkingId) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.ACCESS_VIEW);
        return adminCoworkingAccessService.getCoworkingAccessList(coworkingId);
    }

    public List<CoworkingRoleResponse> getAvailableRoles(Long coworkingId) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.ROLE_VIEW);
        return adminCoworkingAccessService.getAvailableRoles(coworkingId);
    }

    public CoworkingAccessResponse assignRole(Long coworkingId, AssignCoworkingRoleRequest request) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.ROLE_ASSIGN);
        return adminCoworkingAccessService.assignRole(coworkingId, request.getEmail(), request.getRoleId());
    }

    public CoworkingAccessResponse updateRole(Long coworkingId, Long accessId, UpdateCoworkingRoleRequest request) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.ROLE_ASSIGN);
        return adminCoworkingAccessService.updateRole(coworkingId, accessId, request.getRoleId(), request.getActive());
    }

    public void deactivate(Long coworkingId, Long accessId) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.ACCESS_MANAGE);
        adminCoworkingAccessService.deactivate(coworkingId, accessId);
    }
}
