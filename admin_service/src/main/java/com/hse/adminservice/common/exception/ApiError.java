package com.hse.adminservice.common.exception;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record ApiError(
        int status,
        String message,
        LocalDateTime timestamp
) {
}
