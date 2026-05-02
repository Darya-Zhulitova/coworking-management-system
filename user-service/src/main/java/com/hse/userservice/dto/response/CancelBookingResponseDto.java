package com.hse.userservice.dto.response;

public record CancelBookingResponseDto(
        Long bookingId,
        Long coworkingId,
        Long refundMinorUnits,
        Long balanceAfterMinorUnits,
        BookingListItemDto booking
) {
}
