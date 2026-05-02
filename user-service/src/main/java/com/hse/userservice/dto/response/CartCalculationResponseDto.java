package com.hse.userservice.dto.response;

import java.time.LocalDate;
import java.util.List;

public record CartCalculationResponseDto(
        Long coworkingId,
        List<CartCalculatedItemDto> items,
        CartSummaryDto summary
) {
    public record CartCalculatedItemDto(
            Long placeId,
            String placeName,
            LocalDate date,
            String floor,
            String typeName,
            Long tariffId,
            Long basePrice,
            Integer discountPercent,
            Long discountAmount,
            Long finalPrice,
            Boolean available
    ) {
    }

    public record CartSummaryDto(
            Long totalBasePrice,
            Long totalDiscount,
            Long totalFinalPrice,
            Integer unavailableCount,
            List<String> discountHints,
            List<String> validationErrors,
            Boolean hasEnoughBalance,
            Long balanceAfterMinorUnits,
            Boolean canCheckout
    ) {
    }
}
