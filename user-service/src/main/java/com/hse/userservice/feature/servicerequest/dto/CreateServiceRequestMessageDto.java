package com.hse.userservice.feature.servicerequest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateServiceRequestMessageDto(
        @NotBlank(message = "Введите текст сообщения") @Size(
                max = 1000, message = "Текст должен быть не длиннее 1000 символов"
        ) String text
) {
}
