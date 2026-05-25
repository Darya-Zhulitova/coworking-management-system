package com.hse.adminservice.integration.user.dto;

import java.time.LocalDate;
import java.util.List;

public record UserDeactivateOperationRequest(
        String operationType,
        String targetType,
        Long coworkingId,
        String coworkingName,
        Long placeId,
        String placeName,
        Long targetId,
        String targetName,
        List<LocalDate> affectedDates,
        String impactHash
) {
}
