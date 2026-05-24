package com.hse.userservice.feature.booking.service;

import com.hse.userservice.integration.dto.BookingContext;

public record ResolvedPlace(
        BookingContext.Place place,
        BookingContext.PlaceType placeType,
        BookingContext.Tariff tariff,
        BookingContext.Floor floor,
        Long coworkingId
) {
}
