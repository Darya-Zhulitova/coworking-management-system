package com.hse.adminservice.pricing.discount;

import com.hse.adminservice.pricing.discount.domain.TariffDiscountRule;
import com.hse.adminservice.pricing.discount.dto.TariffDiscountRuleRequest;
import com.hse.adminservice.pricing.tariff.domain.Tariff;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TariffDiscountRuleFactory {
    public List<TariffDiscountRule> build(Tariff tariff, List<TariffDiscountRuleRequest> rules) {
        List<TariffDiscountRuleRequest> safeRules = rules == null ? List.of() : rules;
        return safeRules.stream().map(rule -> TariffDiscountRule.builder()
                .tariff(tariff)
                .thresholdQuantity(rule.getThresholdQuantity())
                .discountPercent(rule.getDiscountPercent())
                .build()).toList();
    }
}
