package com.hse.userservice.internal.dto;

import java.time.LocalDate;

public record CoworkingUserReadModelDto(
        Long userId,
        String name,
        LocalDate registeredAt,
        Long balance,
        Integer totalBookings,
        Integer unfinishedBookings
) {
}
