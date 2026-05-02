package com.hse.userservice.internal.dto;

public record AnalyticsMetricPointDto(
        String label,
        Integer value
) {
}
