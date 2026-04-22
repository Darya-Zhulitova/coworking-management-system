package com.hse.adminservice.coworking.dto;

import lombok.Builder;

@Builder
public record AnalyticsMetricPointResponse(
        String label,
        Integer value
) {
}
