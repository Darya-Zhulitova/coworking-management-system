package com.hse.userservice.feature.booking.service;

import com.hse.userservice.feature.booking.domain.Booking;
import com.hse.userservice.feature.booking.dto.BookingInitResponseDto;
import com.hse.userservice.feature.booking.dto.BookingListItemDto;
import com.hse.userservice.integration.dto.BookingContext;
import com.hse.userservice.integration.dto.PlaceSummary;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class BookingResponseMapperTest {
    private final BookingAvailabilityService availabilityService = mock(BookingAvailabilityService.class);
    private final BookingCancellationPolicy cancellationPolicy = mock(BookingCancellationPolicy.class);
    private final BookingResponseMapper mapper = new BookingResponseMapper(availabilityService, cancellationPolicy);
    private final BookingSnapshotContextFactory contextFactory = new BookingSnapshotContextFactory();

    @Test
    void toInitPlaceMapsNamesPriceCoordinatesAmenitiesAndAvailability() {
        BookingSnapshotContext context = contextFactory.from(BookingServiceUnitFixtures.bookingContext());
        BookingContext.Place place = context.placesById().get(100L);
        when(availabilityService.isPlaceAvailable(BookingServiceUnitFixtures.TODAY.plusDays(1), place, context, Map.of()))
                .thenReturn(true);

        BookingInitResponseDto.PlaceItemDto dto = mapper.toInitPlace(
                place,
                BookingServiceUnitFixtures.TODAY.plusDays(1),
                context,
                Map.of()
        );

        assertThat(dto.id()).isEqualTo(100L);
        assertThat(dto.name()).isEqualTo("Place 100");
        assertThat(dto.floorName()).isEqualTo("Floor 10");
        assertThat(dto.placeTypeName()).isEqualTo("Desk");
        assertThat(dto.pricePerDay()).isEqualTo(1_500L);
        assertThat(dto.amenities()).containsExactly("monitor", "coffee");
        assertThat(dto.locX()).isEqualByComparingTo("12.50");
        assertThat(dto.locY()).isEqualByComparingTo("25.00");
        assertThat(dto.imageFileId()).isEqualTo("place-file");
        assertThat(dto.previewImageUrl()).isEqualTo("https://example.test/place-preview.png");
        assertThat(dto.fullImageUrl()).isEqualTo("https://example.test/place.png");
        assertThat(dto.available()).isTrue();
    }

    @Test
    void toInitPlaceUsesFallbackValuesWhenFloorTypeOrTariffIsMissing() {
        BookingContext.Place orphanPlace = BookingServiceUnitFixtures.place(999L, 999L, 999L, true);
        BookingSnapshotContext context = contextFactory.from(BookingServiceUnitFixtures.bookingContext());

        BookingInitResponseDto.PlaceItemDto dto = mapper.toInitPlace(
                orphanPlace,
                BookingServiceUnitFixtures.TODAY.plusDays(1),
                context,
                Map.of()
        );

        assertThat(dto.floorName()).isEqualTo("—");
        assertThat(dto.placeTypeName()).isEqualTo("—");
        assertThat(dto.pricePerDay()).isZero();
        assertThat(dto.available()).isFalse();
        verifyNoInteractions(availabilityService);
    }

    @Test
    void toBookingListItemFromSummaryUsesSummaryPlaceAndCancellationPreviewForActiveBooking() {
        Booking booking = BookingServiceUnitFixtures.booking(5L, 11L, 100L, BookingServiceUnitFixtures.TODAY.plusDays(2), 1_500L);
        when(cancellationPolicy.calculatePreview(booking)).thenReturn(1_200L);

        BookingListItemDto dto = mapper.toBookingListItemFromSummary(
                booking,
                Map.of(100L, new PlaceSummary(
                        100L,
                        "Desk A",
                        "Floor A",
                        "Desk",
                        "https://example.test/place-preview.png",
                        "https://example.test/place.png"
                ))
        );

        assertThat(dto.id()).isEqualTo(5L);
        assertThat(dto.placeName()).isEqualTo("Desk A");
        assertThat(dto.placePreviewImageUrl()).isEqualTo("https://example.test/place-preview.png");
        assertThat(dto.placeFullImageUrl()).isEqualTo("https://example.test/place.png");
        assertThat(dto.cancellationPreviewMinorUnits()).isEqualTo(1_200L);
    }

    @Test
    void toBookingListItemFromSummaryMapsNullImageUrlsWhenSummaryHasNoImages() {
        Booking booking = BookingServiceUnitFixtures.booking(7L, 11L, 100L, BookingServiceUnitFixtures.TODAY.plusDays(2), 1_500L);
        when(cancellationPolicy.calculatePreview(booking)).thenReturn(1_200L);

        BookingListItemDto dto = mapper.toBookingListItemFromSummary(
                booking,
                Map.of(100L, new PlaceSummary(100L, "Desk A", "Floor A", "Desk", null, null))
        );

        assertThat(dto.placePreviewImageUrl()).isNull();
        assertThat(dto.placeFullImageUrl()).isNull();
        assertThat(dto.cancellationPreviewMinorUnits()).isEqualTo(1_200L);
    }

    @Test
    void toBookingListItemDoesNotCalculateCancellationPreviewForInactiveBooking() {
        Booking booking = BookingServiceUnitFixtures.booking(6L, 11L, 100L, BookingServiceUnitFixtures.TODAY.plusDays(2), 1_500L);
        booking.setActive(false);

        BookingListItemDto dto = mapper.toBookingListItem(
                booking,
                Map.of(100L, BookingServiceUnitFixtures.place(100L, 10L, 20L, true))
        );

        assertThat(dto.placeName()).isEqualTo("Place 100");
        assertThat(dto.placePreviewImageUrl()).isEqualTo("https://example.test/place-preview.png");
        assertThat(dto.placeFullImageUrl()).isEqualTo("https://example.test/place.png");
        assertThat(dto.cancellationPreviewMinorUnits()).isZero();
        verifyNoInteractions(cancellationPolicy);
    }
}
