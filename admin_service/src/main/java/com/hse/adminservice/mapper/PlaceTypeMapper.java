package com.hse.adminservice.mapper;

import com.hse.adminservice.dto.PlaceTypeResponse;
import com.hse.adminservice.dto.PlaceTypeSummaryResponse;
import com.hse.adminservice.entity.CoworkingPlaceType;
import org.springframework.stereotype.Component;

@Component
public class PlaceTypeMapper {

    public PlaceTypeResponse toResponse(CoworkingPlaceType placeType) {
        return PlaceTypeResponse.builder()
                .id(placeType.getId())
                .coworkingId(placeType.getCoworking().getId())
                .code(placeType.getCode())
                .name(placeType.getName())
                .description(placeType.getDescription())
                .active(placeType.getActive())
                .archived(placeType.getArchived())
                .archivedAt(placeType.getArchivedAt())
                .createdAt(placeType.getCreatedAt())
                .updatedAt(placeType.getUpdatedAt())
                .build();
    }

    public PlaceTypeSummaryResponse toSummary(CoworkingPlaceType placeType) {
        return PlaceTypeSummaryResponse.builder()
                .id(placeType.getId())
                .code(placeType.getCode())
                .name(placeType.getName())
                .active(placeType.getActive())
                .build();
    }
}
