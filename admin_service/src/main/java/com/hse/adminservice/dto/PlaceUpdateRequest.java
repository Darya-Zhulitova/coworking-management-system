package com.hse.adminservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PlaceUpdateRequest {

    @NotBlank
    private String name;

    private Long placeTypeId;

    private Boolean active;
}
