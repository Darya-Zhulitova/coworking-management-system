package com.hse.adminservice.space.placetype.mapper;

import com.hse.adminservice.pricing.tariff.mapper.TariffMapper;
import com.hse.adminservice.space.placetype.domain.PlaceType;
import com.hse.adminservice.space.placetype.dto.PlaceTypeResponse;
import com.hse.adminservice.space.placetype.dto.PlaceTypeSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PlaceTypeMapper {
    private final TariffMapper tariffMapper;

    public PlaceTypeResponse toResponse(PlaceType placeType) {
        return PlaceTypeResponse.builder()
                .id(placeType.getId())
                .coworkingId(placeType.getCoworking().getId())
                .name(placeType.getName())
                .active(placeType.getActive())
                .archived(placeType.getArchived())
                .archivedAt(placeType.getArchivedAt())
                .createdAt(placeType.getCreatedAt())
                .updatedAt(placeType.getUpdatedAt())
                .tariff(tariffMapper.toResponse(placeType.getTariff()))
                .build();
    }

    public PlaceTypeSummaryResponse toSummary(PlaceType placeType) {
        return PlaceTypeSummaryResponse.builder()
                .id(placeType.getId())
                .name(placeType.getName())
                .active(placeType.getActive())
                .tariffId(placeType.getTariff().getId())
                .tariffName(placeType.getTariff().getName())
                .build();
    }
}
