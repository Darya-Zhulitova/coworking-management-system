package com.hse.adminservice.mapper;

import com.hse.adminservice.dto.PlaceResponse;
import com.hse.adminservice.entity.Place;
import org.springframework.stereotype.Component;

@Component
public class PlaceMapper {

    public PlaceResponse toResponse(Place place) {
        return PlaceResponse.builder()
                .id(place.getId())
                .name(place.getName())
                .type(place.getType())
                .active(place.getActive())
                .archived(place.getArchived())
                .archivedAt(place.getArchivedAt())
                .createdAt(place.getCreatedAt())
                .updatedAt(place.getUpdatedAt())
                .coworkingId(place.getCoworking().getId())
                .build();
    }
}
