package com.hse.userservice.feature.booking.dto;

public record CancelBookingResponseDto(
        Long bookingId,
        Long refundMinorUnits,
        Long balanceAfterMinorUnits,
        BookingListItemDto booking
) {
}
