package com.hse.userservice.feature.booking.service;

import com.hse.userservice.feature.booking.domain.Booking;
import com.hse.userservice.feature.booking.dto.BookingCartItemRequestDto;
import com.hse.userservice.feature.booking.repository.BookingRepository;
import com.hse.userservice.integration.dto.BookingContext;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class BookingAvailabilityServiceTest {
    private final BookingRepository bookingRepository = mock(BookingRepository.class);
    private final BookingAvailabilityService service = new BookingAvailabilityService(bookingRepository);
    private final BookingSnapshotContextFactory contextFactory = new BookingSnapshotContextFactory();

    @Test
    void findReservedPairsReturnsEmptyMapWithoutRepositoryCallWhenInputIsEmpty() {
        assertThat(service.findReservedPairs(List.of())).isEmpty();

        verifyNoInteractions(bookingRepository);
    }

    @Test
    void findReservedPairsBuildsPlaceDateLookupFromActiveBookings() {
        LocalDate date = BookingServiceUnitFixtures.TODAY.plusDays(1);
        Booking booking = BookingServiceUnitFixtures.booking(1L, 11L, 100L, date, 1_500L);
        when(bookingRepository.findAllByPlaceIdInAndDateInAndActiveTrue(any(), any())).thenReturn(List.of(booking));

        Map<String, Boolean> reserved = service.findReservedPairs(List.of(new BookingCartItemRequestDto(100L, date)));

        assertThat(reserved).containsEntry(BookingSnapshotContextFactory.key(100L, date), true);
        verify(bookingRepository).findAllByPlaceIdInAndDateInAndActiveTrue(Set.of(100L), Set.of(date));
    }

    @Test
    void isPlaceAvailableRejectsInactivePlacePlaceClosingClosedScheduleExceptionDisabledWeekdayAndReservedPair() {
        LocalDate monday = LocalDate.of(2026, 5, 25);
        BookingContext.Place activePlace = BookingServiceUnitFixtures.place(100L, 10L, 20L, true);
        BookingContext.Place inactivePlace = BookingServiceUnitFixtures.place(101L, 10L, 20L, false);
        BookingSnapshotContext closedContext = contextFactory.from(new BookingContext(
                1L,
                1L,
                monday.atStartOfDay(),
                31,
                "Coworking",
                true,
                List.of(BookingServiceUnitFixtures.floor(10L, true)),
                List.of(BookingServiceUnitFixtures.tariff(30L, 1_500L, true)),
                List.of(BookingServiceUnitFixtures.placeType(20L, 30L, true)),
                List.of(activePlace),
                List.of(new BookingContext.ScheduleException(1L, monday, "CLOSE", "Closed", true)),
                List.of(new BookingContext.PlaceClosing(1L, 100L, monday.plusDays(1), "Place closed", true))
        ));
        BookingSnapshotContext weekendOnlyContext = contextFactory.from(new BookingContext(
                1L, 1L, monday.atStartOfDay(), 64, "Coworking", true,
                List.of(BookingServiceUnitFixtures.floor(10L, true)),
                List.of(BookingServiceUnitFixtures.tariff(30L, 1_500L, true)),
                List.of(BookingServiceUnitFixtures.placeType(20L, 30L, true)),
                List.of(activePlace), List.of(), List.of()
        ));

        assertThat(service.isPlaceAvailable(monday, inactivePlace, closedContext, Map.of())).isFalse();
        assertThat(service.isPlaceAvailable(monday.plusDays(1), activePlace, closedContext, Map.of())).isFalse();
        assertThat(service.isPlaceAvailable(monday, activePlace, closedContext, Map.of())).isFalse();
        assertThat(service.isPlaceAvailable(monday, activePlace, weekendOnlyContext, Map.of())).isFalse();
        assertThat(service.isPlaceAvailable(monday, activePlace, contextFactory.from(BookingServiceUnitFixtures.bookingContext()), Map.of(
                BookingSnapshotContextFactory.key(100L, monday), true
        ))).isFalse();
    }

    @Test
    void openScheduleExceptionAllowsDateEvenWhenRegularScheduleIsDisabled() {
        LocalDate saturday = LocalDate.of(2026, 5, 23);
        BookingContext.Place place = BookingServiceUnitFixtures.place(100L, 10L, 20L, true);
        BookingSnapshotContext context = contextFactory.from(new BookingContext(
                1L,
                1L,
                saturday.atStartOfDay(),
                31,
                "Coworking",
                true,
                List.of(BookingServiceUnitFixtures.floor(10L, true)),
                List.of(BookingServiceUnitFixtures.tariff(30L, 1_500L, true)),
                List.of(BookingServiceUnitFixtures.placeType(20L, 30L, true)),
                List.of(place),
                List.of(new BookingContext.ScheduleException(1L, saturday, "OPEN", "Special opening", true)),
                List.of()
        ));

        assertThat(service.isPlaceAvailable(saturday, place, context, Map.of())).isTrue();
    }
}
