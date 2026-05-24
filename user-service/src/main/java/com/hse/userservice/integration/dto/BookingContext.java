package com.hse.userservice.integration.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record BookingContext(
        Long coworkingId,
        Long configVersion,
        LocalDateTime generatedAt,
        Integer schedule,
        String name,
        Boolean floorMapEnabled,
        List<Floor> floors,
        List<Tariff> tariffs,
        List<PlaceType> placeTypes,
        List<Place> places,
        List<ScheduleException> scheduleExceptions,
        List<PlaceClosing> placeClosings
) {
    public record Floor(
            Long id,
            String name,
            Integer index,
            String imageFileId,
            String imageUrl,
            Boolean active
    ) {
    }

    public record Tariff(
            Long id,
            String name,
            Long pricePerDay,
            Integer fullRefundHoursBefore,
            Integer lateCancellationRefundPercent,
            BigDecimal cancellationCompensationCoefficient,
            BigDecimal dayClosureCompensationCoefficient,
            BigDecimal membershipBlockCompensationCoefficient,
            Integer version,
            Boolean active
    ) {
    }

    public record PlaceType(
            Long id,
            String name,
            Long tariffId,
            Boolean active
    ) {
    }

    public record Place(
            Long id,
            String name,
            Long floorId,
            Long placeTypeId,
            BigDecimal locX,
            BigDecimal locY,
            String imageFileId,
            String previewImageUrl,
            String fullImageUrl,
            List<String> amenities,
            Boolean active
    ) {
    }

    public record ScheduleException(
            Long id,
            LocalDate date,
            String type,
            String name,
            Boolean active
    ) {
    }

    public record PlaceClosing(
            Long id,
            Long placeId,
            LocalDate date,
            String name,
            Boolean active
    ) {
    }

}
