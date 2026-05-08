package com.hse.adminservice.pricing.tariff.validation;

import com.hse.adminservice.common.error.ConflictException;
import com.hse.adminservice.pricing.discount.TariffDiscountRuleValidator;
import com.hse.adminservice.pricing.discount.dto.TariffDiscountRuleRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
public class TariffPayloadValidator {
    private final TariffDiscountRuleValidator discountRuleValidator;

    public void validate(
            Integer pricePerDay,
            Integer minBookingDays,
            Integer fullRefundHoursBefore,
            Integer lateCancellationRefundPercent,
            BigDecimal cancellationCompensationCoefficient,
            BigDecimal dayClosureCompensationCoefficient,
            BigDecimal membershipBlockCompensationCoefficient,
            List<TariffDiscountRuleRequest> rules
    ) {
        if (pricePerDay == null || pricePerDay < 0) {
            throw new ConflictException("Price per day must be zero or greater");
        }
        if (minBookingDays == null || minBookingDays < 1) {
            throw new ConflictException("Min booking days must be at least 1");
        }
        if (fullRefundHoursBefore == null || fullRefundHoursBefore < 0) {
            throw new ConflictException("Full refund hours before must be zero or greater");
        }
        if (lateCancellationRefundPercent == null || lateCancellationRefundPercent < 0 || lateCancellationRefundPercent > 100) {
            throw new ConflictException("Late cancellation refund percent must be between 0 and 100");
        }
        validateCoefficient("Cancellation compensation coefficient", cancellationCompensationCoefficient);
        validateCoefficient("Day closure compensation coefficient", dayClosureCompensationCoefficient);
        validateCoefficient("Membership block compensation coefficient", membershipBlockCompensationCoefficient);
        discountRuleValidator.validate(rules == null ? List.of() : rules);
    }

    private void validateCoefficient(String label, BigDecimal value) {
        if (value != null && value.compareTo(BigDecimal.ZERO) < 0) {
            throw new ConflictException(label + " must be zero or greater");
        }
    }
}
