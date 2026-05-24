package com.hse.userservice.feature.booking.dto;

import java.time.LocalDate;

public record BookingListItemDto(
        Long id,
        String bookingNumber,
        Long placeId,
        String placeName,
        String placePreviewImageUrl,
        String placeFullImageUrl,
        LocalDate date,
        Long cost,
        Boolean active,
        String status,
        String requestId,
        Long pricePerDay,
        Integer fullRefundHoursBefore,
        Integer lateCancellationRefundPercent,
        Long cancellationPreviewMinorUnits
) {
}
