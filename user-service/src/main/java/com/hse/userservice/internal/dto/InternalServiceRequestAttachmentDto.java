package com.hse.userservice.internal.dto;


public record InternalServiceRequestAttachmentDto(
        Long id,
        String fileName,
        String contentType,
        Long sizeBytes,
        String url
) {
}
