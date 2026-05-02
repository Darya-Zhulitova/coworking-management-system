package com.hse.userservice.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record BookingCartItemRequestDto(
        @NotNull Long placeId,
        @NotNull LocalDate date
) {
}
