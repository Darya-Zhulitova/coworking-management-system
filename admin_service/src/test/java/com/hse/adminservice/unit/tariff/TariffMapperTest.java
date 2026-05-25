package com.hse.adminservice.unit.tariff;

import com.hse.adminservice.coworking.domain.Coworking;
import com.hse.adminservice.pricing.tariff.domain.Tariff;
import com.hse.adminservice.pricing.tariff.dto.TariffResponse;
import com.hse.adminservice.pricing.tariff.mapper.TariffMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class TariffMapperTest {
    private final TariffMapper mapper = new TariffMapper();

    @Test
    void mapsAllFinancialRulesAndLifecycleFields() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 1, 10, 9, 0);
        LocalDateTime updatedAt = LocalDateTime.of(2026, 1, 11, 10, 0);
        LocalDateTime archivedAt = LocalDateTime.of(2026, 1, 12, 11, 0);
        Coworking coworking = Coworking.builder().id(42L).build();
        Tariff tariff = Tariff.builder()
                .id(7L)
                .coworking(coworking)
                .name("Daily")
                .pricePerDay(2500L)
                .fullRefundHoursBefore(48)
                .lateCancellationRefundPercent(60)
                .cancellationCompensationCoefficient(new BigDecimal("1.1000"))
                .dayClosureCompensationCoefficient(new BigDecimal("1.2500"))
                .membershipBlockCompensationCoefficient(new BigDecimal("0.5000"))
                .version(3)
                .active(false)
                .archived(true)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .archivedAt(archivedAt)
                .build();

        TariffResponse response = mapper.toResponse(tariff);

        assertThat(response.id()).isEqualTo(7L);
        assertThat(response.coworkingId()).isEqualTo(42L);
        assertThat(response.name()).isEqualTo("Daily");
        assertThat(response.pricePerDay()).isEqualTo(2500L);
        assertThat(response.fullRefundHoursBefore()).isEqualTo(48);
        assertThat(response.lateCancellationRefundPercent()).isEqualTo(60);
        assertThat(response.cancellationCompensationCoefficient()).isEqualByComparingTo("1.1000");
        assertThat(response.dayClosureCompensationCoefficient()).isEqualByComparingTo("1.2500");
        assertThat(response.membershipBlockCompensationCoefficient()).isEqualByComparingTo("0.5000");
        assertThat(response.version()).isEqualTo(3);
        assertThat(response.active()).isFalse();
        assertThat(response.archived()).isTrue();
        assertThat(response.createdAt()).isEqualTo(createdAt);
        assertThat(response.updatedAt()).isEqualTo(updatedAt);
        assertThat(response.archivedAt()).isEqualTo(archivedAt);
    }
}
