package com.hse.userservice.internal.dto.deactivation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.time.LocalDate;
import java.util.List;

public record ScheduleReductionCommitRequest(
        @NotEmpty List<LocalDate> affectedDates,
        @NotBlank String impactHash
) {
}
