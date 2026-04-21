package com.hse.userservice.internal.dto;

import java.time.LocalDateTime;

public record InternalServiceRequestMessageDto(
        Long messageId,
        Long serviceRequestId,
        String authorType,
        String authorName,
        String text,
        LocalDateTime timestamp,
        LocalDateTime readAt
) {
}
