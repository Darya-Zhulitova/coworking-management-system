package com.hse.adminservice.tariff.mapper;

import com.hse.adminservice.tariff.dto.TariffDiscountRuleResponse;
import com.hse.adminservice.tariff.dto.TariffResponse;
import com.hse.adminservice.tariff.entity.Tariff;
import org.springframework.stereotype.Component;

@Component
public class TariffMapper {
    public TariffResponse toResponse(Tariff tariff) {
        return TariffResponse.builder()
                .id(tariff.getId())
                .coworkingId(tariff.getCoworking().getId())
                .name(tariff.getName())
                .pricePerDay(tariff.getPricePerDay())
                .minBookingDays(tariff.getMinBookingDays())
                .fullRefundHoursBefore(tariff.getFullRefundHoursBefore())
                .lateCancellationRefundPercent(tariff.getLateCancellationRefundPercent())
                .cancellationCompensationCoefficient(tariff.getCancellationCompensationCoefficient())
                .dayClosureCompensationCoefficient(tariff.getDayClosureCompensationCoefficient())
                .membershipBlockCompensationCoefficient(tariff.getMembershipBlockCompensationCoefficient())
                .discountRules(tariff.getDiscountRules()
                        .stream()
                        .sorted(java.util.Comparator.comparing(com.hse.adminservice.tariff.entity.TariffDiscountRule::getThresholdQuantity))
                        .map(rule -> TariffDiscountRuleResponse.builder()
                                .id(rule.getId())
                                .thresholdQuantity(rule.getThresholdQuantity())
                                .discountPercent(rule.getDiscountPercent())
                                .build())
                        .toList())
                .version(tariff.getVersion())
                .active(tariff.getActive())
                .archived(tariff.getArchived())
                .archivedAt(tariff.getArchivedAt())
                .createdAt(tariff.getCreatedAt())
                .updatedAt(tariff.getUpdatedAt())
                .build();
    }
}
