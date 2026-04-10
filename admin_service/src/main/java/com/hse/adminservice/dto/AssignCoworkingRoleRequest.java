package com.hse.adminservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AssignCoworkingRoleRequest {
    @NotBlank
    @Email
    private String email;

    @NotNull
    private Long roleId;
}
