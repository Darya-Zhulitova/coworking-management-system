package com.hse.adminservice.calendar.exception.dto;

import com.hse.adminservice.calendar.exception.domain.ScheduleExceptionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CoworkingScheduleExceptionCreateRequest {
    @NotNull
    private LocalDate date;
    @NotNull
    private ScheduleExceptionType type;
    @NotBlank
    private String name;
}
