package com.hse.adminservice.mapper;

import com.hse.adminservice.dto.PlaceResponse;
import com.hse.adminservice.entity.Place;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PlaceMapper {

    private final PlaceTypeMapper placeTypeMapper;

    public PlaceResponse toResponse(Place place) {
        return PlaceResponse.builder()
                .id(place.getId())
                .name(place.getName())
                .active(place.getActive())
                .archived(place.getArchived())
                .archivedAt(place.getArchivedAt())
                .createdAt(place.getCreatedAt())
                .updatedAt(place.getUpdatedAt())
                .coworkingId(place.getCoworking().getId())
                .placeType(placeTypeMapper.toSummary(place.getPlaceType()))
                .build();
    }
}
