package com.hse.adminservice.dto;

import com.hse.adminservice.entity.Grant;
import lombok.Builder;

import java.util.Set;

@Builder
public record CoworkingAccessResponse(
        Long accessId,
        Long adminId,
        String email,
        String name,
        Long roleId,
        String roleName,
        boolean active,
        Set<Grant> grants
) {
}
