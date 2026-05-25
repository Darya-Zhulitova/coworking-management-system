package com.hse.adminservice.operations.servicedesk.dto;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Builder
public record ServiceRequestMessageResponse(
        Long id,
        String authorType,
        String authorName,
        String text,
        LocalDateTime createdAt,
        List<ServiceRequestAttachmentResponse> attachments
) {
}
