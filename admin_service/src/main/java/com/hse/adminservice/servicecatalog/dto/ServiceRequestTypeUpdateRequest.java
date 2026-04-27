package com.hse.adminservice.servicecatalog.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ServiceRequestTypeUpdateRequest {
    @NotBlank
    private String name;

    @NotNull
    @Min(0)
    private Integer cost;

    private Boolean active;
}
