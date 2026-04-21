package com.hse.userservice.internal.dto;

import java.util.List;

public record UserAnalyticsDto(
        Integer usersCount,
        Integer activeMemberships,
        Integer pendingMemberships,
        Integer blockedMemberships,
        Integer totalBookings,
        Integer activeBookings,
        Integer unfinishedServiceRequests,
        Integer openPayRequests,
        Integer totalBalance,
        Integer monthlyIncome,
        Integer monthlyOccupancyPercent,
        List<AnalyticsMetricPointDto> monthlyIncomeHistory,
        List<AnalyticsMetricPointDto> occupancyHistory
) {
}
