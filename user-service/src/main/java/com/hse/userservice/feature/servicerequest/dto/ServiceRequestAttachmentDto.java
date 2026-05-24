package com.hse.userservice.feature.servicerequest.dto;


public record ServiceRequestAttachmentDto(
        Long id,
        String fileName,
        String contentType,
        Long sizeBytes,
        String url
) {
}
