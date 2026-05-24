package com.hse.userservice.feature.booking.service;

import com.hse.userservice.integration.dto.BookingContext;

import java.time.LocalDate;
import java.util.Map;

public record BookingSnapshotContext(
        Map<Long, BookingContext.Floor> floorsById,
        Map<Long, BookingContext.PlaceType> placeTypesById,
        Map<Long, BookingContext.Tariff> tariffsById,
        Map<Long, BookingContext.Place> placesById,
        Map<LocalDate, BookingContext.ScheduleException> scheduleExceptionsByDate,
        Map<String, BookingContext.PlaceClosing> placeClosingsByPlaceAndDate,
        int scheduleBitmask
) {
}
