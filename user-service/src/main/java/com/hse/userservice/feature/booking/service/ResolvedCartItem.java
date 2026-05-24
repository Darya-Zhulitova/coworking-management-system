package com.hse.userservice.feature.booking.service;

import com.hse.userservice.integration.dto.BookingContext;

import java.time.LocalDate;

public record ResolvedCartItem(
        Long placeId,
        String placeName,
        LocalDate date,
        String floorName,
        String typeName,
        BookingContext.Tariff tariff,
        long finalPrice,
        boolean available
) {
}
