package com.hse.adminservice.rbac.dto;

import com.hse.adminservice.rbac.entity.Grant;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Set;

@Data
public class RoleCreateRequest {
    @NotBlank
    private String name;
    @NotEmpty
    private Set<@NotNull Grant> grants;
    private Boolean active = true;
}
