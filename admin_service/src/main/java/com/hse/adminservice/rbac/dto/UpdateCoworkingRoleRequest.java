package com.hse.adminservice.rbac.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateCoworkingRoleRequest {
    @NotNull
    private Long roleId;

    @NotNull
    private Boolean active;
}
