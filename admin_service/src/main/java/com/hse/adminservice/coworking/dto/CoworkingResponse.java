package com.hse.adminservice.coworking.dto;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Builder
public record CoworkingResponse(
        Long id,
        String name,
        String description,
        String address,
        String workingHoursLabel,
        String heroTitle,
        String heroText,
        List<String> imageUrls,
        Integer schedule,
        Boolean autoApproveMembership,
        Boolean active,
        Boolean archived,
        Long configurationVersion,
        LocalDateTime archivedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
