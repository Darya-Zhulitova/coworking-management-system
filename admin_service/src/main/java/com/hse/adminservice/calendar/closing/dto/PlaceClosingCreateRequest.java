package com.hse.adminservice.calendar.closing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class PlaceClosingCreateRequest {
    @NotNull
    private Long placeId;
    @NotNull
    private LocalDate date;
    @NotBlank
    private String name;
    private String impactHash;
}
