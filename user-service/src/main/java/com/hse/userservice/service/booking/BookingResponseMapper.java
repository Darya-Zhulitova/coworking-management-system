package com.hse.userservice.service.booking;

import com.hse.userservice.client.dto.CoworkingConfigSnapshot;
import com.hse.userservice.domain.booking.Booking;
import com.hse.userservice.dto.response.BookingInitResponseDto;
import com.hse.userservice.dto.response.BookingListItemDto;
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
            CoworkingConfigSnapshot.Place place,
            LocalDate previewDate,
            BookingSnapshotContext context,
            Map<String, Boolean> reservedPairs
    ) {
        CoworkingConfigSnapshot.Floor floor = context.floorsById().get(place.floorId());
        CoworkingConfigSnapshot.PlaceType placeType = context.placeTypesById().get(place.placeTypeId());
        CoworkingConfigSnapshot.Tariff tariff = placeType == null ? null : context.tariffsById()
                .get(placeType.tariffId());
        boolean previewAvailable = tariff != null && bookingAvailabilityService.isPlaceAvailable(
                previewDate,
                place,
                context,
                reservedPairs
        );

        return new BookingInitResponseDto.PlaceItemDto(
                place.id(),
                place.name(),
                floor == null ? "—" : floor.name(),
                placeType == null ? "—" : placeType.name(),
                tariff == null ? null : tariff.id(),
                tariff == null ? 0 : tariff.pricePerDay(),
                place.amenities(),
                place.active(),
                previewAvailable
        );
    }

    public BookingListItemDto toBookingListItem(Booking booking, Map<Long, CoworkingConfigSnapshot.Place> placesById) {
        CoworkingConfigSnapshot.Place place = placesById.get(booking.getPlaceId());
        return new BookingListItemDto(
                booking.getId(),
                booking.getCoworkingId(),
                booking.getPlaceId(),
                place == null ? "Unknown place" : place.name(),
                booking.getDate(),
                booking.getCost(),
                booking.getActive(),
                booking.getStatus().name(),
                booking.getRequestId(),
                booking.getTariffId(),
                booking.getPricePerDay(),
                booking.getAppliedDiscountPercent(),
                booking.getFullRefundHoursBefore(),
                booking.getActive() ? bookingCancellationPolicy.calculatePreview(booking) : 0L
        );
    }
}
