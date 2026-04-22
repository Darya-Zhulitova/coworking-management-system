package com.hse.adminservice.placetype.dto;

import jakarta.validation.constraints.NotBlank;

public record PlaceTypeUpdateRequest(
        @NotBlank String name,
        Boolean active
) {
}
