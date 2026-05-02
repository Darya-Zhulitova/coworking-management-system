package com.hse.userservice.dto.response;

import java.time.LocalDate;

public record BookingListItemDto(
        Long id,
        Long coworkingId,
        Long placeId,
        String placeName,
        LocalDate date,
        Long cost,
        Boolean active,
        String status,
        String requestId,
        Long tariffId,
        Integer pricePerDay,
        Integer appliedDiscountPercent,
        Integer fullRefundHoursBefore,
        Integer lateCancellationRefundPercent,
        Long cancellationPreviewMinorUnits
) {
}
