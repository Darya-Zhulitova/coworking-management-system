package com.hse.userservice.internal.dto.deactivation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record DayClosingCommitRequest(
        @NotNull LocalDate date,
        @NotBlank String impactHash
) {
}
