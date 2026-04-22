package com.hse.adminservice.placetype.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PlaceTypeCreateRequest(
        @NotBlank String name,
        @NotNull Long tariffId
) {
}
