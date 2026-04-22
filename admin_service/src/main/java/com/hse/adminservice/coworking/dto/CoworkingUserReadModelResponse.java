package com.hse.adminservice.coworking.dto;

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
