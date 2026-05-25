package com.hse.adminservice.operations.users.dto;

import lombok.Builder;

import java.time.LocalDate;

@Builder
public record CoworkingUserReadModelResponse(
        Long userId,
        String name,
        LocalDate registeredAt,
        Long balance,
        Integer totalBookings,
        Integer unfinishedBookings
) {
}
