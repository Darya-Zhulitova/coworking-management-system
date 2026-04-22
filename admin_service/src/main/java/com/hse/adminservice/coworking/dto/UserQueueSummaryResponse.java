package com.hse.adminservice.coworking.dto;

import lombok.Builder;

@Builder
public record UserQueueSummaryResponse(
        Integer usersCount,
        Integer pendingMemberships,
        Integer pendingPayRequests,
        Integer openServiceRequests,
        Integer totalBookings,
        Integer activeBookings,
        Integer currentBalance,
        Integer monthlyIncome,
        Integer monthlyOccupancyPercent
) {
}
