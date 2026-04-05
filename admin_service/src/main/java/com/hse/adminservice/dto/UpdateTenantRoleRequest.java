package com.hse.adminservice.dto;

import com.hse.adminservice.entity.AdminCoworkingRole;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateTenantRoleRequest {
    @NotNull
    private AdminCoworkingRole role;

    @NotNull
    private Boolean active;
}
