package com.hse.userservice.feature.membership.dto;

import java.util.List;

public record JoinCoworkingPreviewDto(
        Long coworkingId,
        String name,
        String description,
        String address,
        String workingHoursLabel,
        String heroTitle,
        String heroText,
        List<String> imageUrls,
        boolean autoApproveMembership,
        boolean active
) {
}
