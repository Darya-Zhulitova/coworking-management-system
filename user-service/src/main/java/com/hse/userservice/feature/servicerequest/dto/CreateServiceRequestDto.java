package com.hse.userservice.feature.servicerequest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateServiceRequestDto(
        @NotNull(message = "Выберите тип сервисной заявки") Long typeId,

        @NotBlank(message = "Укажите название") @Size(
                max = 255, message = "Название должно быть не длиннее 255 символов"
        ) String name
) {
}
