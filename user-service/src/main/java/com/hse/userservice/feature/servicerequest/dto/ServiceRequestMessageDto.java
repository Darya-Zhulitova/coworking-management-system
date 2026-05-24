package com.hse.userservice.feature.servicerequest.dto;

import com.hse.userservice.feature.servicerequest.domain.MessageAuthorType;

import java.time.LocalDateTime;
import java.util.List;

public record ServiceRequestMessageDto(
        Long id,
        Long requestId,
        MessageAuthorType authorType,
        String authorName,
        String text,
        LocalDateTime timestamp,
        LocalDateTime readAt,
        List<ServiceRequestAttachmentDto> attachments
) {
}
