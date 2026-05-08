package com.hse.adminservice.pricing.discount.dto;

import lombok.Builder;

@Builder
public record TariffDiscountRuleResponse(
        Long id,
        Integer thresholdQuantity,
        Integer discountPercent
) {
}
