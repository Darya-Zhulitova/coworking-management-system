package com.hse.userservice.feature.booking.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record CartCalculateRequestDto(
        @Valid @NotEmpty List<BookingCartItemRequestDto> items
) {
}
