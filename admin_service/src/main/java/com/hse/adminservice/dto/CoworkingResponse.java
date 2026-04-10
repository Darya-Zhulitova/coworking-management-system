package com.hse.adminservice.dto;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class CoworkingResponse {
    Long id;
    String name;
    Integer schedule;
    Boolean active;
    Boolean archived;
    Long configurationVersion;
    LocalDateTime archivedAt;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
