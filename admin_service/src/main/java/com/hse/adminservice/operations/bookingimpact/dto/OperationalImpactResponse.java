package com.hse.adminservice.operations.bookingimpact.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record OperationalImpactResponse(
        Integer affectedBookingsCount,
        List<String> affectedDates,
        List<AffectedBookingResponse> affectedBookings,
        Long totalCompensationAmount,
        String impactHash
) {
}
