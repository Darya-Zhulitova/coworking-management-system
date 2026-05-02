package com.hse.userservice.internal.dto.deactivation;

import java.util.List;

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
