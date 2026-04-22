package com.hse.adminservice.coworking.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record UserAnalyticsResponse(
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
        List<AnalyticsMetricPointResponse> monthlyIncomeHistory,
        List<AnalyticsMetricPointResponse> occupancyHistory
) {
}
