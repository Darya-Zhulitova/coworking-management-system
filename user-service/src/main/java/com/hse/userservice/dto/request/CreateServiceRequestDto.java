package com.hse.userservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateServiceRequestDto(
        @NotNull(message = "membershipId must not be null")
        Long membershipId,

        Long placeId,

        Long bookingId,

        @NotBlank(message = "category must not be blank")
        @Size(max = 64, message = "category must be at most 64 characters")
        String category,

        @NotBlank(message = "description must not be blank")
        @Size(max = 1000, message = "description must be at most 1000 characters")
        String description
) {
}