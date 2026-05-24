package com.hse.userservice.feature.booking.service;

import com.hse.userservice.feature.booking.domain.Booking;
import com.hse.userservice.feature.booking.dto.BookingInitResponseDto;
import com.hse.userservice.feature.booking.dto.BookingListItemDto;
import com.hse.userservice.integration.dto.BookingContext;
import com.hse.userservice.integration.dto.PlaceSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class BookingResponseMapper {
    private final BookingAvailabilityService bookingAvailabilityService;
    private final BookingCancellationPolicy bookingCancellationPolicy;

    public BookingInitResponseDto.PlaceItemDto toInitPlace(
            BookingContext.Place place,
            LocalDate previewDate,
            BookingSnapshotContext context,
            Map<String, Boolean> reservedPairs
    ) {
        BookingContext.Floor floor = context.floorsById().get(place.floorId());
        BookingContext.PlaceType placeType = context.placeTypesById().get(place.placeTypeId());
        BookingContext.Tariff tariff = placeType == null ? null : context.tariffsById().get(placeType.tariffId());
        boolean available = tariff != null && bookingAvailabilityService.isPlaceAvailable(
                previewDate,
                place,
                context,
                reservedPairs
        );

        return new BookingInitResponseDto.PlaceItemDto(
                place.id(),
                place.name(),
                place.floorId(),
                floor == null ? "—" : floor.name(),
                place.placeTypeId(),
                placeType == null ? "—" : placeType.name(),
                tariff == null ? 0L : tariff.pricePerDay(),
                place.amenities(),
                place.locX(),
                place.locY(),
                place.imageFileId(),
                place.previewImageUrl(),
                place.fullImageUrl(),
                place.active(),
                available
        );
    }

    public BookingListItemDto toBookingListItemFromSummary(Booking booking, Map<Long, PlaceSummary> placesById) {
        PlaceSummary place = placesById.get(booking.getPlaceId());
        return new BookingListItemDto(
                booking.getId(),
                booking.getBookingNumber(),
                booking.getPlaceId(),
                place == null ? "Unknown place" : place.name(),
                place == null ? null : place.previewImageUrl(),
                place == null ? null : place.fullImageUrl(),
                booking.getDate(),
                booking.getCost(),
                booking.getActive(),
                booking.getStatus().name(),
                booking.getRequestId(),
                booking.getPricePerDay(),
                booking.getFullRefundHoursBefore(),
                booking.getLateCancellationRefundPercent(),
                booking.getActive() ? bookingCancellationPolicy.calculatePreview(booking) : 0L
        );
    }

    public BookingListItemDto toBookingListItem(Booking booking, Map<Long, BookingContext.Place> placesById) {
        BookingContext.Place place = placesById.get(booking.getPlaceId());
        return new BookingListItemDto(
                booking.getId(),
                booking.getBookingNumber(),
                booking.getPlaceId(),
                place == null ? "Unknown place" : place.name(),
                place == null ? null : place.previewImageUrl(),
                place == null ? null : place.fullImageUrl(),
                booking.getDate(),
                booking.getCost(),
                booking.getActive(),
                booking.getStatus().name(),
                booking.getRequestId(),
                booking.getPricePerDay(),
                booking.getFullRefundHoursBefore(),
                booking.getLateCancellationRefundPercent(),
                booking.getActive() ? bookingCancellationPolicy.calculatePreview(booking) : 0L
        );
    }
}
