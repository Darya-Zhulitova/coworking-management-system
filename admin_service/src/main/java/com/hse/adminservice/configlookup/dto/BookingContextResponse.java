package com.hse.adminservice.configlookup.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Builder
public record BookingContextResponse(
        Long coworkingId,
        Long configVersion,
        LocalDateTime generatedAt,
        Integer schedule,
        String name,
        Boolean floorMapEnabled,
        List<FloorDto> floors,
        List<TariffDto> tariffs,
        List<PlaceTypeDto> placeTypes,
        List<PlaceDto> places,
        List<ScheduleExceptionDto> scheduleExceptions,
        List<PlaceClosingDto> placeClosings
) {
    @Builder
    public record FloorDto(
            Long id,
            String name,
            Integer index,
            String imageFileId,
            String imageUrl,
            Boolean active
    ) {
    }

    @Builder
    public record TariffDto(
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

    @Builder
    public record PlaceTypeDto(
            Long id,
            String name,
            Long tariffId,
            Boolean active
    ) {
    }

    @Builder
    public record PlaceDto(
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

    @Builder
    public record ScheduleExceptionDto(
            Long id,
            LocalDate date,
            String type,
            String name,
            Boolean active
    ) {
    }

    @Builder
    public record PlaceClosingDto(
            Long id,
            Long placeId,
            LocalDate date,
            String name,
            Boolean active
    ) {
    }
}
