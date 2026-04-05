package com.hse.adminservice.mapper;

import com.hse.adminservice.authorization.EffectiveAdminAction;
import com.hse.adminservice.dto.TenantRoleResponse;
import com.hse.adminservice.dto.TenantStaffMemberResponse;
import com.hse.adminservice.entity.AdminCoworkingAccess;
import com.hse.adminservice.entity.AdminCoworkingAssignmentType;
import com.hse.adminservice.entity.AdminCoworkingRole;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class TenantStaffMapper {

    public TenantStaffMemberResponse toResponse(AdminCoworkingAccess access, Set<EffectiveAdminAction> grantedActions) {
        return TenantStaffMemberResponse.builder()
                .accessId(access.getId())
                .adminUserId(access.getAdminUser().getId())
                .email(access.getAdminUser().getEmail())
                .assignmentType(access.getAssignmentType())
                .role(access.getRole())
                .owner(access.getAssignmentType() == AdminCoworkingAssignmentType.OWNER)
                .active(Boolean.TRUE.equals(access.getActive()))
                .grantedActions(grantedActions)
                .build();
    }

    public TenantRoleResponse toRoleResponse(AdminCoworkingRole role, String label, Set<EffectiveAdminAction> grantedActions) {
        return TenantRoleResponse.builder()
                .code(role)
                .label(label)
                .grantedActions(grantedActions)
                .build();
    }
}
