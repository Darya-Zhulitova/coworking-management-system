package com.hse.adminservice.dto.internal;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.List;

@Value
@Builder
public class CoworkingConfigSnapshotResponse {
    Long coworkingId;
    Long configVersion;
    LocalDateTime generatedAt;
    List<SnapshotPlaceTypeDto> placeTypes;
    List<SnapshotPlaceDto> places;

    @Value
    @Builder
    public static class SnapshotPlaceTypeDto {
        Long id;
        String code;
        String name;
        Boolean active;
    }

    @Value
    @Builder
    public static class SnapshotPlaceDto {
        Long id;
        String name;
        Boolean active;
        Long placeTypeId;
    }
}
