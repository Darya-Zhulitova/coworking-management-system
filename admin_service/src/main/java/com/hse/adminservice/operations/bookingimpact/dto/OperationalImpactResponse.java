package com.hse.adminservice.operations.bookingimpact.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record OperationalImpactResponse(
        String operationType,
        String targetType,
        Long targetId,
        String targetName,
        Integer simulatedAffectedFutureBookings,
        List<String> plannedUserDomainCommands,
        List<String> affectedDates,
        List<AffectedBookingResponse> affectedBookings,
        Integer totalCompensationAmount,
        String mode,
        String summary
) {
}
