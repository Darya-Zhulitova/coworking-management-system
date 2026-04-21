package com.hse.userservice.dto.response;

import java.util.List;

public record CoworkingDetailsDto(
        Long id,
        String name,
        String description,
        String address,
        String workingHoursLabel,
        String heroTitle,
        String heroText,
        List<String> imageUrls,
        Boolean autoApproveMembership,
        Boolean active,
        Long membershipId,
        String membershipStatus,
        Long balanceMinorUnits
) {
}
