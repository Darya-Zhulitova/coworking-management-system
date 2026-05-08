package com.hse.adminservice.coworking.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record CoworkingPublicInfoResponse(
        Long id,
        String name,
        String description,
        String address,
        String workingHoursLabel,
        String heroTitle,
        String heroText,
        List<String> imageUrls,
        Boolean autoApproveMembership,
        Boolean active
) {
}
