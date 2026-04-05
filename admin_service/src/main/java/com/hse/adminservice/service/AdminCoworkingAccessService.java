package com.hse.adminservice.service;

import com.hse.adminservice.authorization.EffectiveAdminAction;
import com.hse.adminservice.authorization.SystemTenantRoleDefinitions;
import com.hse.adminservice.authorization.TenantPermission;
import com.hse.adminservice.dto.AccessibleCoworkingResponse;
import com.hse.adminservice.dto.TenantRoleResponse;
import com.hse.adminservice.dto.TenantStaffMemberResponse;
import com.hse.adminservice.entity.*;
import com.hse.adminservice.exception.ConflictException;
import com.hse.adminservice.exception.ResourceNotFoundException;
import com.hse.adminservice.mapper.TenantStaffMapper;
import com.hse.adminservice.repository.AdminCoworkingAccessRepository;
import com.hse.adminservice.repository.AdminUserRepository;
import com.hse.adminservice.repository.CoworkingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminCoworkingAccessService {

    private final AdminCoworkingAccessRepository accessRepository;
    private final AdminUserRepository adminUserRepository;
    private final CoworkingRepository coworkingRepository;
    private final SystemTenantRoleDefinitions roleDefinitions;
    private final TenantStaffMapper tenantStaffMapper;

    public List<AccessibleCoworkingResponse> getAccessibleCoworkings(Long adminUserId) {
        return accessRepository.findAllByAdminUserIdAndActiveTrueAndCoworkingArchivedFalse(adminUserId).stream()
                .map(access -> AccessibleCoworkingResponse.builder()
                        .id(access.getCoworking().getId())
                        .name(access.getCoworking().getName())
                        .assignmentType(access.getAssignmentType())
                        .role(access.getRole())
                        .owner(access.getAssignmentType() == AdminCoworkingAssignmentType.OWNER)
                        .build())
                .toList();
    }

    @Transactional
    public AdminCoworkingAccess grantOwnerAccess(AdminUser adminUser, Coworking coworking) {
        return accessRepository.findByAdminUserIdAndCoworkingId(adminUser.getId(), coworking.getId())
                .map(existingAccess -> {
                    existingAccess.setAssignmentType(AdminCoworkingAssignmentType.OWNER);
                    existingAccess.setRole(null);
                    existingAccess.setActive(true);
                    existingAccess.setUpdatedAt(LocalDateTime.now());
                    return accessRepository.save(existingAccess);
                })
                .orElseGet(() -> accessRepository.save(AdminCoworkingAccess.builder()
                        .adminUser(adminUser)
                        .coworking(coworking)
                        .assignmentType(AdminCoworkingAssignmentType.OWNER)
                        .role(null)
                        .active(true)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build()));
    }

    public List<TenantStaffMemberResponse> getTenantStaff(Long coworkingId) {
        return accessRepository.findAllByCoworkingIdAndCoworkingArchivedFalse(coworkingId).stream()
                .map(this::toStaffResponse)
                .toList();
    }

    public List<TenantRoleResponse> getAvailableRoles() {
        return List.of(
                toRoleResponse(AdminCoworkingRole.MANAGER, "Manager"),
                toRoleResponse(AdminCoworkingRole.STAFF_SUPPORT, "Staff support")
        );
    }

    @Transactional
    public TenantStaffMemberResponse assignRole(Long coworkingId, String email, AdminCoworkingRole role) {
        AdminUser adminUser = adminUserRepository.findByEmailIgnoreCaseAndArchivedFalse(email.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Admin user not found"));

        if (accessRepository.findByAdminUserIdAndCoworkingId(adminUser.getId(), coworkingId).isPresent()) {
            throw new ConflictException("Admin already has access to this coworking");
        }

        Coworking coworking = coworkingRepository.findByIdAndArchivedFalse(coworkingId)
                .orElseThrow(() -> new ResourceNotFoundException("Coworking not found"));

        AdminCoworkingAccess access = accessRepository.save(AdminCoworkingAccess.builder()
                .adminUser(adminUser)
                .coworking(coworking)
                .assignmentType(AdminCoworkingAssignmentType.ROLE_ASSIGNED)
                .role(role)
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());

        return accessRepository.findByIdAndCoworkingIdAndCoworkingArchivedFalse(access.getId(), coworkingId)
                .map(this::toStaffResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Staff access not found"));
    }

    @Transactional
    public TenantStaffMemberResponse updateRole(Long coworkingId, Long accessId, AdminCoworkingRole role, Boolean active) {
        AdminCoworkingAccess access = accessRepository.findByIdAndCoworkingIdAndCoworkingArchivedFalse(accessId, coworkingId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff access not found"));

        if (access.getAssignmentType() == AdminCoworkingAssignmentType.OWNER) {
            throw new ConflictException("Owner access cannot be changed through staff role API");
        }

        access.setRole(role);
        access.setActive(active);
        access.setUpdatedAt(LocalDateTime.now());
        AdminCoworkingAccess saved = accessRepository.save(access);

        return accessRepository.findByIdAndCoworkingIdAndCoworkingArchivedFalse(saved.getId(), coworkingId)
                .map(this::toStaffResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Staff access not found"));
    }

    @Transactional
    public void deactivate(Long coworkingId, Long accessId) {
        AdminCoworkingAccess access = accessRepository.findByIdAndCoworkingIdAndCoworkingArchivedFalse(accessId, coworkingId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff access not found"));

        if (access.getAssignmentType() == AdminCoworkingAssignmentType.OWNER) {
            throw new ConflictException("Owner access cannot be deactivated through staff role API");
        }

        access.setActive(false);
        access.setUpdatedAt(LocalDateTime.now());
        accessRepository.save(access);
    }

    public Set<EffectiveAdminAction> resolveGrantedActions(AdminCoworkingAccess access) {
        if (!Boolean.TRUE.equals(access.getActive())) {
            return EnumSet.noneOf(EffectiveAdminAction.class);
        }
        if (access.getAssignmentType() == AdminCoworkingAssignmentType.OWNER) {
            return EnumSet.of(
                    EffectiveAdminAction.VIEW_ACCESSIBLE_COWORKINGS,
                    EffectiveAdminAction.VIEW_COWORKING,
                    EffectiveAdminAction.UPDATE_COWORKING,
                    EffectiveAdminAction.ARCHIVE_COWORKING,
                    EffectiveAdminAction.VIEW_TENANT_DASHBOARD,
                    EffectiveAdminAction.VIEW_STAFF_ACCESS,
                    EffectiveAdminAction.MANAGE_STAFF_ACCESS,
                    EffectiveAdminAction.VIEW_PLACES,
                    EffectiveAdminAction.MANAGE_PLACES
            );
        }

        Set<TenantPermission> permissions = roleDefinitions.getPermissions(access.getRole());
        EnumSet<EffectiveAdminAction> actions = EnumSet.of(EffectiveAdminAction.VIEW_ACCESSIBLE_COWORKINGS);
        if (permissions.contains(TenantPermission.VIEW_COWORKING_DETAILS)) actions.add(EffectiveAdminAction.VIEW_COWORKING);
        if (permissions.contains(TenantPermission.EDIT_COWORKING_DETAILS)) actions.add(EffectiveAdminAction.UPDATE_COWORKING);
        if (permissions.contains(TenantPermission.ARCHIVE_COWORKING)) actions.add(EffectiveAdminAction.ARCHIVE_COWORKING);
        if (permissions.contains(TenantPermission.VIEW_TENANT_DASHBOARD)) actions.add(EffectiveAdminAction.VIEW_TENANT_DASHBOARD);
        if (permissions.contains(TenantPermission.VIEW_STAFF_ACCESS)) actions.add(EffectiveAdminAction.VIEW_STAFF_ACCESS);
        if (permissions.contains(TenantPermission.MANAGE_STAFF_ACCESS)) actions.add(EffectiveAdminAction.MANAGE_STAFF_ACCESS);
        if (permissions.contains(TenantPermission.VIEW_PLACES)) actions.add(EffectiveAdminAction.VIEW_PLACES);
        if (permissions.contains(TenantPermission.MANAGE_PLACES)) actions.add(EffectiveAdminAction.MANAGE_PLACES);
        return actions;
    }

    public Set<TenantPermission> resolvePermissions(AdminCoworkingAccess access) {
        if (!Boolean.TRUE.equals(access.getActive())) {
            return EnumSet.noneOf(TenantPermission.class);
        }
        if (access.getAssignmentType() == AdminCoworkingAssignmentType.OWNER) {
            return EnumSet.allOf(TenantPermission.class);
        }
        return roleDefinitions.getPermissions(access.getRole());
    }

    private TenantStaffMemberResponse toStaffResponse(AdminCoworkingAccess access) {
        return tenantStaffMapper.toResponse(access, resolveGrantedActions(access));
    }

    private TenantRoleResponse toRoleResponse(AdminCoworkingRole role, String label) {
        AdminCoworkingAccess syntheticAccess = AdminCoworkingAccess.builder()
                .assignmentType(AdminCoworkingAssignmentType.ROLE_ASSIGNED)
                .role(role)
                .active(true)
                .build();
        return tenantStaffMapper.toRoleResponse(role, label, resolveGrantedActions(syntheticAccess));
    }
}
