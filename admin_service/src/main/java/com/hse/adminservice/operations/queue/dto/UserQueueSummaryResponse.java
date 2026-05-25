package com.hse.adminservice.operations.queue.dto;

import lombok.Builder;

@Builder
public record UserQueueSummaryResponse(
        Integer usersCount,
        Integer pendingMemberships,
        Integer pendingPayRequests,
        Integer openServiceRequests,
        Integer totalBookings,
        Integer activeBookings,
        Long currentBalance,
        Long monthlyIncome,
        Integer monthlyOccupancyPercent
) {
}
