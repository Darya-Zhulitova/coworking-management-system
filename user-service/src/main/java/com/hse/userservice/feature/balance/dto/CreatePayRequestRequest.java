package com.hse.userservice.feature.balance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreatePayRequestRequest(
        @NotNull(message = "Укажите сумму") Long amount,

        @NotBlank(message = "Укажите комментарий") @Size(
                max = 1000, message = "Комментарий должен быть не длиннее 1000 символов"
        ) String userComment
) {
}
