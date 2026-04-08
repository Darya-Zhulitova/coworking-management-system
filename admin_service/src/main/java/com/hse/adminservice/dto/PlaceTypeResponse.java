package com.hse.adminservice.dto;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class PlaceTypeResponse {
    Long id;
    Long coworkingId;
    String code;
    String name;
    String description;
    Boolean active;
    Boolean archived;
    LocalDateTime archivedAt;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
