package com.hse.userservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateServiceRequestDto(
        @NotNull(message = "coworkingId must not be null") Long coworkingId,

        @NotNull(message = "typeId must not be null") Long typeId,

        @NotBlank(message = "name must not be blank") @Size(
                max = 255,
                message = "name must be at most 255 characters"
        ) String name
) {
}
