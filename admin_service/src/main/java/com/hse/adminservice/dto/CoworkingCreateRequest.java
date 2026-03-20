package com.hse.adminservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CoworkingCreateRequest {

    @NotBlank
    private String name;
}