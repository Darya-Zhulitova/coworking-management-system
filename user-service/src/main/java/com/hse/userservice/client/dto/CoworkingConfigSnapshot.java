package com.hse.userservice.client.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record CoworkingConfigSnapshot(
        Long coworkingId,
        Long configVersion,
        LocalDateTime generatedAt,
        Integer schedule,
        String name,
        String description,
        String address,
        String workingHoursLabel,
        String heroTitle,
        String heroText,
        List<String> imageUrls,
        List<Floor> floors,
        List<Tariff> tariffs,
        List<PlaceType> placeTypes,
        List<Place> places,
        List<ScheduleException> scheduleExceptions,
        List<PlaceClosing> placeClosings,
        List<ServiceRequestType> serviceRequestTypes
) {
    public record Floor(
            Long id,
            String name,
            Integer index,
            String imageFileId,
            Boolean active
    ) {
    }

    public record TariffDiscountRule(
            Long id,
            Integer thresholdQuantity,
            Integer discountPercent
    ) {
    }

    public record Tariff(
            Long id,
            String name,
            Integer pricePerDay,
            Integer minBookingDays,
            Integer fullRefundHoursBefore,
            Integer lateCancellationRefundPercent,
            BigDecimal cancellationCompensationCoefficient,
            BigDecimal dayClosureCompensationCoefficient,
            BigDecimal membershipBlockCompensationCoefficient,
            List<TariffDiscountRule> discountRules,
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

    public record ServiceRequestType(
            Long id,
            String name,
            Integer cost,
            Integer version,
            Boolean active
    ) {
    }
}
