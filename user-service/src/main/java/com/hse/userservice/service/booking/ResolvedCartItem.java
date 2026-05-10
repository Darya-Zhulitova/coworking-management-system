package com.hse.userservice.service.booking;

import com.hse.userservice.client.dto.CoworkingConfigSnapshot;

import java.time.LocalDate;

public record ResolvedCartItem(
        Long placeId,
        String placeName,
        LocalDate date,
        String floorName,
        String typeName,
        CoworkingConfigSnapshot.Tariff tariff,
        long basePrice,
        int discountPercent,
        long discountAmount,
        long finalPrice,
        boolean available
) {
}
