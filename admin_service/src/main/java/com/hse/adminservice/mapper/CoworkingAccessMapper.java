package com.hse.adminservice.mapper;

import com.hse.adminservice.entity.Grant;
import com.hse.adminservice.dto.CoworkingRoleResponse;
import com.hse.adminservice.dto.CoworkingAccessResponse;
import com.hse.adminservice.entity.Access;
import com.hse.adminservice.entity.Role;
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
                .build();
    }
}
