package com.hse.adminservice.operations.servicedesk.dto;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record ServiceRequestMessageResponse(
        Long messageId,
        Long serviceRequestId,
        String authorType,
        String authorName,
        String text,
        LocalDateTime timestamp,
        LocalDateTime readAt
) {
}
