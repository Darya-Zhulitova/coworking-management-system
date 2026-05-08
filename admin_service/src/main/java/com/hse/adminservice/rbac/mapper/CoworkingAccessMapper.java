package com.hse.adminservice.rbac.mapper;

import com.hse.adminservice.rbac.domain.Access;
import com.hse.adminservice.rbac.domain.Grant;
import com.hse.adminservice.rbac.domain.Role;
import com.hse.adminservice.rbac.dto.CoworkingAccessResponse;
import com.hse.adminservice.rbac.dto.CoworkingRoleResponse;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class CoworkingAccessMapper {

    public CoworkingAccessResponse toResponse(Access access, Set<Grant> grants) {
        return CoworkingAccessResponse.builder()
                .accessId(access.getId())
                .adminId(access.getAdmin().getId())
                .email(access.getAdmin().getEmail())
                .name(access.getAdmin().getName())
                .roleId(access.getRole().getId())
                .roleName(access.getRole().getName())
                .active(Boolean.TRUE.equals(access.getActive()))
                .grants(grants)
                .build();
    }

    public CoworkingRoleResponse toRoleResponse(Role role, Set<Grant> grants) {
        return CoworkingRoleResponse.builder()
                .roleId(role.getId())
                .name(role.getName())
                .grants(grants)
                .active(Boolean.TRUE.equals(role.getActive()))
                .build();
    }
}
