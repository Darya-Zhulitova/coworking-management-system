package com.hse.userservice.internal.dto.deactivation;

import java.util.List;

public record OperationalImpactResponse(
        Integer affectedBookingsCount,
        List<String> affectedDates,
        List<AffectedBookingResponse> affectedBookings,
        Long totalCompensationAmount,
        String impactHash
) {
}
