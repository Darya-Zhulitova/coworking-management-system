package com.hse.adminservice.operations.users.dto;

import lombok.Builder;

import java.time.LocalDate;

@Builder
public record CoworkingUserReadModelResponse(
        Long userId,
        String name,
        LocalDate registeredAt,
        Integer balance,
        Integer totalBookings,
        Integer unfinishedBookings
) {
}
