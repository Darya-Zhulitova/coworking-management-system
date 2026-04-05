package com.hse.adminservice.dto;

import com.hse.adminservice.authorization.EffectiveAdminAction;
import com.hse.adminservice.entity.AdminCoworkingAssignmentType;
import com.hse.adminservice.entity.AdminCoworkingRole;
import lombok.Builder;

import java.util.Set;

@Builder
public record TenantStaffMemberResponse(
        Long accessId,
        Long adminUserId,
        String email,
        AdminCoworkingAssignmentType assignmentType,
        AdminCoworkingRole role,
        boolean owner,
        boolean active,
        Set<EffectiveAdminAction> grantedActions
) {
}
