package com.hse.adminservice.space.floor.dto;

import jakarta.validation.constraints.NotBlank;

public record FloorUpdateRequest(
        @NotBlank String name,
        String imageFileId,
        Boolean active
) {
}
