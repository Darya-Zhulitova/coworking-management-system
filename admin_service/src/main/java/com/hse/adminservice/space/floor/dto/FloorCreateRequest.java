package com.hse.adminservice.space.floor.dto;

import jakarta.validation.constraints.NotBlank;

public record FloorCreateRequest(
        @NotBlank String name,
        String imageFileId
) {
}
