package com.hse.userservice.internal.dto.deactivation;

import jakarta.validation.constraints.NotNull;

public record PlaceDeactivationPreviewRequest(@NotNull Long placeId) {
}
