package com.hse.adminservice.floor.mapper;

import com.hse.adminservice.floor.dto.FloorResponse;
import com.hse.adminservice.floor.entity.Floor;
import org.springframework.stereotype.Component;

@Component
public class FloorMapper {
    public FloorResponse toResponse(Floor floor) {
        return FloorResponse.builder()
                .id(floor.getId())
                .coworkingId(floor.getCoworking().getId())
                .name(floor.getName())
                .index(floor.getIndex())
                .imageFileId(floor.getImageFileId())
                .active(floor.getActive())
                .archived(floor.getArchived())
                .archivedAt(floor.getArchivedAt())
                .createdAt(floor.getCreatedAt())
                .updatedAt(floor.getUpdatedAt())
                .build();
    }
}
