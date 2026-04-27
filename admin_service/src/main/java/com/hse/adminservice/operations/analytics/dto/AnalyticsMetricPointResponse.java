package com.hse.adminservice.operations.analytics.dto;

import lombok.Builder;

@Builder
public record AnalyticsMetricPointResponse(
        String label,
        Integer value
) {
}
