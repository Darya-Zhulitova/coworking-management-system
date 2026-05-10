package com.hse.userservice.service.booking;

import com.hse.userservice.client.dto.CoworkingConfigSnapshot;

import java.time.LocalDate;
import java.util.Map;

public record BookingSnapshotContext(
        Map<Long, CoworkingConfigSnapshot.Floor> floorsById,
        Map<Long, CoworkingConfigSnapshot.PlaceType> placeTypesById,
        Map<Long, CoworkingConfigSnapshot.Tariff> tariffsById,
        Map<Long, CoworkingConfigSnapshot.Place> placesById,
        Map<LocalDate, CoworkingConfigSnapshot.ScheduleException> scheduleExceptionsByDate,
        Map<String, CoworkingConfigSnapshot.PlaceClosing> placeClosingsByPlaceAndDate,
        int scheduleBitmask
) {
}
