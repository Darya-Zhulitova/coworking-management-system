package com.hse.adminservice.operations.servicedesk.dto;

public record ServiceRequestAttachmentResponse(
        Long id,
        String fileName,
        String contentType,
        Long sizeBytes,
        String url
) {
}
