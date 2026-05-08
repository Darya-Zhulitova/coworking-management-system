package com.hse.adminservice.rbac.application;

import com.hse.adminservice.coworking.domain.Coworking;
import com.hse.adminservice.coworking.dto.CoworkingListItemResponse;
import com.hse.adminservice.rbac.authorization.GrantResolver;
import com.hse.adminservice.rbac.domain.Access;
import com.hse.adminservice.rbac.domain.Grant;
import com.hse.adminservice.rbac.dto.CoworkingAccessResponse;
import com.hse.adminservice.rbac.dto.CoworkingRoleResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccessService {
    private final AccessibleCoworkingQueryService accessibleCoworkingQueryService;
    private final CoworkingStaffAccessService coworkingStaffAccessService;
    private final CoworkingRoleService coworkingRoleService;
    private final GrantResolver grantResolver;

    public List<CoworkingListItemResponse> getAccessibleCoworkings(Long adminId) {
        return accessibleCoworkingQueryService.getAccessibleCoworkings(adminId);
    }

    public String resolveDisplayAccessLabel(Long adminId, Coworking coworking) {
        return accessibleCoworkingQueryService.resolveDisplayAccessLabel(adminId, coworking);
    }

    public List<CoworkingAccessResponse> getCoworkingAccessList(Long coworkingId) {
        return coworkingStaffAccessService.getCoworkingAccessList(coworkingId);
    }

    public List<CoworkingRoleResponse> getAllRoles(Long coworkingId) {
        return coworkingRoleService.getAllRoles(coworkingId);
    }

    public List<CoworkingRoleResponse> getAvailableRoles(Long coworkingId) {
        return coworkingRoleService.getAvailableRoles(coworkingId);
    }

    public CoworkingRoleResponse getRole(Long coworkingId, Long roleId) {
        return coworkingRoleService.getRole(coworkingId, roleId);
    }

    @Transactional
    public CoworkingRoleResponse createRole(Long coworkingId, String name, Set<Grant> grants, Boolean active) {
        return coworkingRoleService.createRole(coworkingId, name, grants, active);
    }

    @Transactional
    public CoworkingRoleResponse updateRoleDefinition(
            Long coworkingId,
            Long roleId,
            String name,
            Set<Grant> grants,
            Boolean active
    ) {
        return coworkingRoleService.updateRoleDefinition(coworkingId, roleId, name, grants, active);
    }

    @Transactional
    public void archiveRole(Long coworkingId, Long roleId) {
        coworkingRoleService.archiveRole(coworkingId, roleId);
    }

    @Transactional
    public CoworkingAccessResponse assignRole(Long coworkingId, String email, Long roleId) {
        return coworkingStaffAccessService.assignRole(coworkingId, email, roleId);
    }

    @Transactional
    public CoworkingAccessResponse updateAccessRole(Long coworkingId, Long accessId, Long roleId, Boolean active) {
        return coworkingStaffAccessService.updateAccessRole(coworkingId, accessId, roleId, active);
    }

    @Transactional
    public void deactivate(Long coworkingId, Long accessId) {
        coworkingStaffAccessService.deactivate(coworkingId, accessId);
    }

    public Set<Grant> resolveGrantedActions(Access access) {
        return grantResolver.resolveGrantedActions(access);
    }

    public Set<Grant> resolveOwnerGrantedActions() {
        return grantResolver.resolveOwnerGrantedActions();
    }
}
