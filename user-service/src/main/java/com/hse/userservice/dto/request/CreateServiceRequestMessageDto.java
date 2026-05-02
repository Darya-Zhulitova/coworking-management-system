package com.hse.userservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateServiceRequestMessageDto(
        @NotNull(message = "coworkingId must not be null") Long coworkingId,

        @NotBlank(message = "text must not be blank") @Size(
                max = 1000,
                message = "text must be at most 1000 characters"
        ) String text
) {
}
