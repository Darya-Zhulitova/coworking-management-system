package com.hse.adminservice.dto;

import com.hse.adminservice.entity.PlaceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PlaceCreateRequest {

    @NotBlank
    private String name;

    @NotNull
    private PlaceType type;
}
