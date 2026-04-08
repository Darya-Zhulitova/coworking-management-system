package com.hse.adminservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PlaceTypeUpdateRequest {

    @NotBlank
    private String code;

    @NotBlank
    private String name;

    private String description;

    private Boolean active;
}
