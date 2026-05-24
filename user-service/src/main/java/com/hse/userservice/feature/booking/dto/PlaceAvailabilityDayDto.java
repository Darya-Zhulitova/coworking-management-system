package com.hse.userservice.feature.booking.dto;

import java.time.LocalDate;

public record PlaceAvailabilityDayDto(
        LocalDate date,
        Boolean available
) {
}
