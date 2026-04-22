package com.hse.adminservice.floor.dto;

import jakarta.validation.constraints.NotBlank;

public record FloorCreateRequest(
        @NotBlank String name,
        String imageFileId
) {
}
