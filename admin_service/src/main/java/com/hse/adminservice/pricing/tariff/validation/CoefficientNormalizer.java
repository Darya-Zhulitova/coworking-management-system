package com.hse.adminservice.pricing.tariff.validation;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class CoefficientNormalizer {
    public BigDecimal normalize(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value.stripTrailingZeros();
    }
}
