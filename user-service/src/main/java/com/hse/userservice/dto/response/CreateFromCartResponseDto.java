package com.hse.userservice.dto.response;

import java.util.List;

public record CreateFromCartResponseDto(
        Long coworkingId,
        String requestId,
        Long totalChargedMinorUnits,
        Long balanceAfterMinorUnits,
        List<BookingListItemDto> bookings
) {
}
