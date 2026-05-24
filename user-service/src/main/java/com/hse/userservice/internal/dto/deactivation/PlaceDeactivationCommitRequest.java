package com.hse.userservice.internal.dto.deactivation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PlaceDeactivationCommitRequest(
        @NotNull Long placeId,
        @NotBlank String impactHash
) {
}
