package com.hse.adminservice.pricing.discount.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class TariffDiscountRuleRequest {
    @Min(1)
    private Integer thresholdQuantity;

    @Min(0)
    @Max(100)
    private Integer discountPercent;
}
