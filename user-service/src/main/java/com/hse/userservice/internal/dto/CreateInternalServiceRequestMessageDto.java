package com.hse.userservice.internal.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateInternalServiceRequestMessageDto(
        @NotBlank String text
) {
}
