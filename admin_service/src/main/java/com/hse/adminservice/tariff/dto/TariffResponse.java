package com.hse.adminservice.tariff.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Builder
public record TariffResponse(
        Long id,
        Long coworkingId,
        String name,
        Integer pricePerDay,
        Integer minBookingDays,
        Integer fullRefundHoursBefore,
        Integer lateCancellationRefundPercent,
        BigDecimal cancellationCompensationCoefficient,
        BigDecimal dayClosureCompensationCoefficient,
        BigDecimal membershipBlockCompensationCoefficient,
        List<TariffDiscountRuleResponse> discountRules,
        Integer version,
        Boolean active,
        Boolean archived,
        LocalDateTime archivedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
