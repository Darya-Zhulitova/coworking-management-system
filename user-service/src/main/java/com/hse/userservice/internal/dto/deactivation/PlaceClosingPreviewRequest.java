package com.hse.userservice.internal.dto.deactivation;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record PlaceClosingPreviewRequest(
        @NotNull Long placeId,
        @NotNull LocalDate date
) {
}
