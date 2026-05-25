package com.hse.adminservice.support;

import com.hse.adminservice.coworking.domain.Coworking;
import com.hse.adminservice.pricing.tariff.domain.Tariff;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public final class TariffTestFactory {
    private TariffTestFactory() {
    }

    public static Tariff tariff(Coworking coworking) {
        LocalDateTime now = LocalDateTime.now();
        return Tariff.builder()
                .coworking(coworking)
                .name("Daily " + UUID.randomUUID())
                .pricePerDay(1_500L)
                .fullRefundHoursBefore(24)
                .lateCancellationRefundPercent(50)
                .cancellationCompensationCoefficient(new BigDecimal("1.0000"))
                .dayClosureCompensationCoefficient(new BigDecimal("1.0000"))
                .membershipBlockCompensationCoefficient(new BigDecimal("1.0000"))
                .version(1)
                .active(true)
                .archived(false)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }
}
