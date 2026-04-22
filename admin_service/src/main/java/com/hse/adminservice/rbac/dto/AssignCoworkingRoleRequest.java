package com.hse.adminservice.rbac.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AssignCoworkingRoleRequest(
        @NotBlank @Email String email,
        @NotNull Long roleId
) {
}
