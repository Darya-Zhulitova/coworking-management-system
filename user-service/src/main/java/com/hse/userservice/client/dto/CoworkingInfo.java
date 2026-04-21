package com.hse.userservice.client.dto;

import java.util.List;

public record CoworkingInfo(
        Long id,
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
