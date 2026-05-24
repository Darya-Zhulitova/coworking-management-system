package com.hse.userservice.internal.dto;


public record UserQueueSummaryDto(
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
