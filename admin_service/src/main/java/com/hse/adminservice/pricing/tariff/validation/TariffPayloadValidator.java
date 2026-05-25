package com.hse.adminservice.pricing.tariff.validation;

import com.hse.adminservice.common.error.ConflictException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class TariffPayloadValidator {
    public void validate(
            Long pricePerDay,
            Integer fullRefundHoursBefore,
            Integer lateCancellationRefundPercent,
            BigDecimal cancellationCompensationCoefficient,
            BigDecimal dayClosureCompensationCoefficient,
            BigDecimal membershipBlockCompensationCoefficient
    ) {
        if (pricePerDay == null || pricePerDay < 0) {
            throw new ConflictException("Цена за день не может быть отрицательной");
        }
        if (fullRefundHoursBefore == null || fullRefundHoursBefore < 0) {
            throw new ConflictException("Количество часов для полного возврата не может быть отрицательным");
        }
        if (lateCancellationRefundPercent == null || lateCancellationRefundPercent < 0 || lateCancellationRefundPercent > 100) {
            throw new ConflictException("Процент возврата при поздней отмене должен быть от 0 до 100");
        }
        validateCoefficient("Коэффициент компенсации при отмене", cancellationCompensationCoefficient);
        validateCoefficient("Коэффициент компенсации при закрытии дня", dayClosureCompensationCoefficient);
        validateCoefficient(
                "Коэффициент компенсации при блокировке пользователя",
                membershipBlockCompensationCoefficient
        );
    }

    private void validateCoefficient(String label, BigDecimal value) {
        if (value != null && value.compareTo(BigDecimal.ZERO) < 0) {
            throw new ConflictException(label + " не может быть отрицательным");
        }
    }
}
