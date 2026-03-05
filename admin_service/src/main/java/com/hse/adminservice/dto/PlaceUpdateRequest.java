package com.hse.adminservice.dto;

import com.hse.adminservice.entity.PlaceType;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PlaceUpdateRequest {

    @NotBlank
    private String name;

    private PlaceType type;

    private Boolean active;
}
