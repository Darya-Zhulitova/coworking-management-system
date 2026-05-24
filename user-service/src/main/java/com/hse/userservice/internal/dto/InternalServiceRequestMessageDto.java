package com.hse.userservice.internal.dto;

import java.time.LocalDateTime;
import java.util.List;

public record InternalServiceRequestMessageDto(
        Long id,
        String authorType,
        String authorName,
        String text,
        LocalDateTime createdAt,
        List<InternalServiceRequestAttachmentDto> attachments
) {
}
