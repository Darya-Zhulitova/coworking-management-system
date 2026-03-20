package com.hse.adminservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CoworkingUpdateRequest {

    @NotBlank
    private String name;

    private Boolean active;
}