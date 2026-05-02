package com.hse.userservice.internal.dto;

public record UserQueueSummaryDto(
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
