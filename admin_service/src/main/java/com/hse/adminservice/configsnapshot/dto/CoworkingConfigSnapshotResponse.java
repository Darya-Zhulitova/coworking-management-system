package com.hse.adminservice.configsnapshot.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Builder
public record CoworkingConfigSnapshotResponse(
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
        List<SnapshotFloorDto> floors,
        List<SnapshotTariffDto> tariffs,
        List<SnapshotPlaceTypeDto> placeTypes,
        List<SnapshotPlaceDto> places,
        List<SnapshotScheduleExceptionDto> scheduleExceptions,
        List<SnapshotPlaceClosingDto> placeClosings,
        List<SnapshotServiceRequestTypeDto> serviceRequestTypes
) {
    @Builder
    public record SnapshotFloorDto(
            Long id,
            String name,
            Integer index,
            String imageFileId,
            Boolean active
    ) {
    }

    @Builder
    public record SnapshotTariffDiscountRuleDto(
            Long id,
            Integer thresholdQuantity,
            Integer discountPercent
    ) {
    }

    @Builder
    public record SnapshotTariffDto(
            Long id,
            String name,
            Integer pricePerDay,
            Integer minBookingDays,
            Integer fullRefundHoursBefore,
            Integer lateCancellationRefundPercent,
            BigDecimal cancellationCompensationCoefficient,
            BigDecimal dayClosureCompensationCoefficient,
            BigDecimal membershipBlockCompensationCoefficient,
            List<SnapshotTariffDiscountRuleDto> discountRules,
            Integer version,
            Boolean active
    ) {
    }

    @Builder
    public record SnapshotPlaceTypeDto(
            Long id,
            String name,
            Long tariffId,
            Boolean active
    ) {
    }

    @Builder
    public record SnapshotPlaceDto(
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

    @Builder
    public record SnapshotScheduleExceptionDto(
            Long id,
            LocalDate date,
            String type,
            String name,
            Boolean active
    ) {
    }

    @Builder
    public record SnapshotPlaceClosingDto(
            Long id,
            Long placeId,
            LocalDate date,
            String name,
            Boolean active
    ) {
    }

    @Builder
    public record SnapshotServiceRequestTypeDto(
            Long id,
            String name,
            Integer cost,
            Integer version,
            Boolean active
    ) {
    }
}
