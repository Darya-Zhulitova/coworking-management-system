package com.hse.userservice.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CartCalculateRequestDto(
        @NotNull Long coworkingId,
        @Valid @NotEmpty List<BookingCartItemRequestDto> items
) {
}
