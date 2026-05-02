package com.hse.userservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreatePayRequestDto(
        @NotNull(message = "coworkingId must not be null") @Positive(message = "coworkingId must be positive") Long coworkingId,

        @NotNull(message = "amount must not be null") Long amount,

        @NotBlank(message = "userComment must not be blank") @Size(
                max = 1000,
                message = "userComment must be at most 1000 characters"
        ) String userComment
) {
}
