package com.hse.adminservice.tariff.dto;

import lombok.Builder;

@Builder
public record TariffDiscountRuleResponse(
        Long id,
        Integer thresholdQuantity,
        Integer discountPercent
) {
}
