package com.hse.userservice.internal.dto.deactivation;

import jakarta.validation.constraints.NotEmpty;

import java.time.LocalDate;
import java.util.List;

public record ScheduleReductionPreviewRequest(@NotEmpty List<LocalDate> affectedDates) {
}
