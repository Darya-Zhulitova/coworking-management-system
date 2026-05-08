package com.hse.adminservice.space.floor.dto;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record FloorResponse(
        Long id,
        Long coworkingId,
        String name,
        Integer index,
        String imageFileId,
        Boolean active,
        Boolean archived,
        LocalDateTime archivedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
