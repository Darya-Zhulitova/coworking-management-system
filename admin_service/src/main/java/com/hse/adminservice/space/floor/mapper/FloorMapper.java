package com.hse.adminservice.space.floor.mapper;

import com.hse.adminservice.files.FileStorageService;
import com.hse.adminservice.space.floor.domain.Floor;
import com.hse.adminservice.space.floor.dto.FloorResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FloorMapper {
    private final FileStorageService fileStorageService;

    public FloorResponse toResponse(Floor floor) {
        String imageKey = floor.getFullImageFileId() != null ? floor.getFullImageFileId() : floor.getImageFileId();
        return FloorResponse.builder()
                .id(floor.getId())
                .coworkingId(floor.getCoworking().getId())
                .name(floor.getName())
                .index(floor.getIndex())
                .imageFileId(imageKey)
                .imageUrl(fileStorageService.presignedUrl(imageKey))
                .active(floor.getActive())
                .archived(floor.getArchived())
                .archivedAt(floor.getArchivedAt())
                .createdAt(floor.getCreatedAt())
                .updatedAt(floor.getUpdatedAt())
                .build();
    }
}
