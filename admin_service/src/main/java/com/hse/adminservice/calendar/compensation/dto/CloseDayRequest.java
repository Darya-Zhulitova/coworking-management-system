package com.hse.adminservice.calendar.compensation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CloseDayRequest {
    @NotNull
    private LocalDate date;

    @NotBlank
    private String name;
}
