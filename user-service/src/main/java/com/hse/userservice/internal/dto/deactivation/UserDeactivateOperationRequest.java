package com.hse.userservice.internal.dto.deactivation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record UserDeactivateOperationRequest(
        @NotBlank String operationType,
        @NotBlank String targetType,
        @NotNull Long coworkingId,
        String coworkingName,
        Long placeId,
        String placeName,
        @NotNull Long targetId,
        @NotBlank String targetName,
        List<LocalDate> affectedDates
) {
}
