package com.hse.adminservice.place.mapper;

import com.hse.adminservice.place.dto.PlaceResponse;
import com.hse.adminservice.place.entity.Place;
import com.hse.adminservice.placetype.mapper.PlaceTypeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

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
                .floorId(place.getFloor().getId())
                .floorName(place.getFloor().getName())
                .locX(place.getLocX())
                .locY(place.getLocY())
                .amenities(parseAmenities(place.getAmenitiesRaw()))
                .placeType(placeTypeMapper.toSummary(place.getPlaceType()))
                .build();
    }

    public List<String> parseAmenities(String amenitiesRaw) {
        if (amenitiesRaw == null || amenitiesRaw.isBlank()) {
            return List.of();
        }
        return Arrays.stream(amenitiesRaw.split(",")).map(String::trim).filter(value -> !value.isBlank()).toList();
    }

    public String serializeAmenities(List<String> amenities) {
        if (amenities == null || amenities.isEmpty()) {
            return null;
        }
        return amenities.stream()
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .reduce((left, right) -> left + "," + right)
                .orElse(null);
    }
}
