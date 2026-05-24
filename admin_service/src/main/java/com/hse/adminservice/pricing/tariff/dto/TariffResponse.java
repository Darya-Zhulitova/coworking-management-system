package com.hse.adminservice.pricing.tariff.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record TariffResponse(
        Long id,
        Long coworkingId,
        String name,
        Long pricePerDay,
        Integer fullRefundHoursBefore,
        Integer lateCancellationRefundPercent,
        BigDecimal cancellationCompensationCoefficient,
        BigDecimal dayClosureCompensationCoefficient,
        BigDecimal membershipBlockCompensationCoefficient,
        Integer version,
        Boolean active,
        Boolean archived,
        LocalDateTime archivedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
