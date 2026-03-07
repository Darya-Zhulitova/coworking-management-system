package com.hse.userservice.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateMembershipDto(
        @NotNull(message = "coworkingId must not be null")
        @Positive(message = "coworkingId must be positive")
        Long coworkingId
) {
}