package com.hse.userservice.dto.response;

import com.hse.userservice.domain.message.MessageAuthorType;

import java.time.LocalDateTime;

public record ServiceRequestMessageDto(
        Long id,
        Long requestId,
        MessageAuthorType authorType,
        String authorName,
        String text,
        LocalDateTime timestamp,
        LocalDateTime readAt
) {
}
