package com.hse.adminservice.unit.tariff;

import com.hse.adminservice.pricing.tariff.validation.CoefficientNormalizer;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class CoefficientNormalizerTest {
    private final CoefficientNormalizer normalizer = new CoefficientNormalizer();

    @Test
    void convertsMissingCoefficientToZero() {
        assertThat(normalizer.normalize(null)).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void stripsTrailingZerosWithoutChangingNumericValue() {
        BigDecimal normalized = normalizer.normalize(new BigDecimal("1.2500"));

        assertThat(normalized).isEqualByComparingTo("1.25");
        assertThat(normalized.scale()).isEqualTo(2);
    }
}
