package com.hse.adminservice.dto;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class PlaceResponse {
    Long id;
    String name;
    Boolean active;
    Boolean archived;
    LocalDateTime archivedAt;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    Long coworkingId;
    PlaceTypeSummaryResponse placeType;
}
