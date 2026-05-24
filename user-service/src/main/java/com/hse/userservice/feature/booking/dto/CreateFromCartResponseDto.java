package com.hse.userservice.feature.booking.dto;

import java.util.List;

public record CreateFromCartResponseDto(
        String requestId,
        Long totalChargedMinorUnits,
        Long balanceAfterMinorUnits,
        List<BookingListItemDto> bookings
) {
}
