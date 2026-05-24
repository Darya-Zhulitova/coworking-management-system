package com.hse.userservice.internal.dto;


public record OperationsDashboardDto(
        Queues queues,
        Memberships memberships,
        Finance finance,
        Occupancy occupancy
) {
    public record Queues(
            Integer pendingMemberships,
            Integer pendingPayRequests,
            Integer openServiceRequests
    ) {
    }

    public record Memberships(
            Integer total,
            Integer active,
            Integer pending,
            Integer blocked
    ) {
    }

    public record Finance(
            Long totalBalance,
            Long monthlyIncome
    ) {
    }

    public record Occupancy(
            Integer monthlyPercent
    ) {
    }
}
