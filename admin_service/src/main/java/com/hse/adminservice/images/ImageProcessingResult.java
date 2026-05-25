package com.hse.adminservice.images;

public record ImageProcessingResult(
        byte[] full,
        byte[] preview
) {
}
