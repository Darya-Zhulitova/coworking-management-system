package com.hse.adminservice.dto;

import com.hse.adminservice.entity.PlaceType;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class PlaceResponse {
    Long id;
    String name;
    PlaceType type;
    Boolean active;
    Boolean archived;
    LocalDateTime archivedAt;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    Long coworkingId;
}
