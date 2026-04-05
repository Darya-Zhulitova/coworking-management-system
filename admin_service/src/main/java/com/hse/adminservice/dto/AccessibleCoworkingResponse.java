package com.hse.adminservice.dto;

import com.hse.adminservice.entity.AdminCoworkingAssignmentType;
import com.hse.adminservice.entity.AdminCoworkingRole;
import lombok.Builder;

@Builder
public record AccessibleCoworkingResponse(
        Long id,
        String name,
        AdminCoworkingAssignmentType assignmentType,
        AdminCoworkingRole role,
        boolean owner
) {
}
