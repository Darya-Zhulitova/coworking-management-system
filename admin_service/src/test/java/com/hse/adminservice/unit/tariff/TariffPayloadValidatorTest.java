package com.hse.adminservice.unit.tariff;

import com.hse.adminservice.common.error.ConflictException;
import com.hse.adminservice.pricing.tariff.validation.TariffPayloadValidator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TariffPayloadValidatorTest {
    private final TariffPayloadValidator validator = new TariffPayloadValidator();

    @Test
    void acceptsBoundaryValuesAllowedByDomainRules() {
        assertThatCode(() -> validator.validate(
                0L,
                0,
                0,
                BigDecimal.ZERO,
                new BigDecimal("1.0000"),
                null
        )).doesNotThrowAnyException();

        assertThatCode(() -> validator.validate(
                1L,
                24,
                100,
                new BigDecimal("0.0000"),
                new BigDecimal("2.5000"),
                new BigDecimal("0.7500")
        )).doesNotThrowAnyException();
    }

    @Test
    void rejectsInvalidMoneyAndRefundRulesBeforePersistence() {
        assertThatThrownBy(() -> validator.validate(-1L, 24, 50, null, null, null))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Цена за день не может быть отрицательной");

        assertThatThrownBy(() -> validator.validate(1000L, -1, 50, null, null, null))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Количество часов для полного возврата не может быть отрицательным");
    }

    @Test
    void rejectsInvalidRefundPercentAndNegativeCompensationCoefficients() {
        assertThatThrownBy(() -> validator.validate(1000L, 24, 101, null, null, null))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Процент возврата при поздней отмене должен быть от 0 до 100");

        assertThatThrownBy(() -> validator.validate(
                1000L,
                24,
                50,
                new BigDecimal("-0.0001"),
                null,
                null
        )).isInstanceOf(ConflictException.class)
                .hasMessageContaining("Коэффициент компенсации при отмене не может быть отрицательным");

        assertThatThrownBy(() -> validator.validate(
                1000L,
                24,
                50,
                null,
                new BigDecimal("-1.0000"),
                null
        )).isInstanceOf(ConflictException.class)
                .hasMessageContaining("Коэффициент компенсации при закрытии дня не может быть отрицательным");

        assertThatThrownBy(() -> validator.validate(
                1000L,
                24,
                50,
                null,
                null,
                new BigDecimal("-0.5000")
        )).isInstanceOf(ConflictException.class)
                .hasMessageContaining("Коэффициент компенсации при блокировке пользователя не может быть отрицательным");
    }
}
