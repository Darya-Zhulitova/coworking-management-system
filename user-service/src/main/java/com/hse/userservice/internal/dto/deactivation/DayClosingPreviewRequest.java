package com.hse.userservice.internal.dto.deactivation;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record DayClosingPreviewRequest(@NotNull LocalDate date) {
}
