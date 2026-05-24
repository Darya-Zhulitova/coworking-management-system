package com.hse.userservice.feature.booking.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record BookingCartItemRequestDto(
        @NotNull Long placeId,
        @NotNull LocalDate date
) {
}
