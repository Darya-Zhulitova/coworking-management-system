package com.hse.adminservice.schedule.dto;

import com.hse.adminservice.schedule.entity.ScheduleExceptionType;
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
