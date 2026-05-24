package com.hse.userservice.feature.booking.dto;

import java.time.LocalDate;
import java.util.List;

public record CartCalculationResponseDto(
        List<CartCalculatedItemDto> items,
        CartSummaryDto summary
) {
    public record CartCalculatedItemDto(
            Long placeId,
            String placeName,
            LocalDate date,
            String floor,
            String typeName,
            Long finalPrice,
            Boolean available
    ) {
    }

    public record CartSummaryDto(
            Long totalFinalPrice,
            Integer unavailableCount,
            List<String> validationErrors,
            Boolean hasEnoughBalance,
            Long balanceAfterMinorUnits,
            Boolean canCheckout
    ) {
    }
}
