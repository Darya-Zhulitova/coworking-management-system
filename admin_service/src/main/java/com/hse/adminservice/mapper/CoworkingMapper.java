package com.hse.adminservice.mapper;

import com.hse.adminservice.dto.CoworkingResponse;
import com.hse.adminservice.entity.Coworking;
import org.springframework.stereotype.Component;

@Component
public class CoworkingMapper {

    public CoworkingResponse toResponse(Coworking coworking) {
        return CoworkingResponse.builder()
                .id(coworking.getId())
                .name(coworking.getName())
                .active(coworking.getActive())
                .archived(coworking.getArchived())
                .configurationVersion(coworking.getConfigurationVersion())
                .archivedAt(coworking.getArchivedAt())
                .createdAt(coworking.getCreatedAt())
                .updatedAt(coworking.getUpdatedAt())
                .build();
    }
}
