package com.hse.adminservice.pricing.discount;

import com.hse.adminservice.common.error.ConflictException;
import com.hse.adminservice.pricing.discount.dto.TariffDiscountRuleRequest;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TariffDiscountRuleValidator {
    public void validate(List<TariffDiscountRuleRequest> rules) {
        Integer previousThreshold = null;
        Integer previousDiscount = null;
        for (TariffDiscountRuleRequest rule : rules == null ? List.<TariffDiscountRuleRequest>of() : rules) {
            if (rule.getThresholdQuantity() == null || rule.getDiscountPercent() == null) {
                throw new ConflictException("Every discount rule must include threshold quantity and discount percent");
            }
            if (rule.getThresholdQuantity() < 1) {
                throw new ConflictException("Discount threshold quantity must be at least 1");
            }
            if (rule.getDiscountPercent() < 0 || rule.getDiscountPercent() > 100) {
                throw new ConflictException("Discount percent must be between 0 and 100");
            }
            if (previousThreshold != null && rule.getThresholdQuantity() <= previousThreshold) {
                throw new ConflictException("Discount rules must be ordered by strictly increasing threshold quantity");
            }
            if (previousDiscount != null && rule.getDiscountPercent() <= previousDiscount) {
                throw new ConflictException("Discount percent must strictly increase as threshold quantity grows");
            }
            previousThreshold = rule.getThresholdQuantity();
            previousDiscount = rule.getDiscountPercent();
        }
    }
}
