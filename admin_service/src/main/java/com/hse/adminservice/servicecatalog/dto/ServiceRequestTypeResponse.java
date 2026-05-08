package com.hse.adminservice.servicecatalog.dto;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class ServiceRequestTypeResponse {
    Long id;
    Long coworkingId;
    String name;
    Integer cost;
    Integer version;
    Boolean active;
    Boolean archived;
    LocalDateTime archivedAt;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
