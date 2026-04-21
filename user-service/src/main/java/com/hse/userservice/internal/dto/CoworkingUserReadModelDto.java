package com.hse.userservice.internal.dto;

import java.time.LocalDate;

public record CoworkingUserReadModelDto(
        Long userId,
        String name,
        LocalDate registeredAt,
        Integer balance,
        Integer totalBookings,
        Integer unfinishedBookings
) {
}
