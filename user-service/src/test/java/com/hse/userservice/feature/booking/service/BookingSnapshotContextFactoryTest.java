package com.hse.userservice.feature.booking.service;

import com.hse.userservice.integration.dto.BookingContext;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BookingSnapshotContextFactoryTest {
    private final BookingSnapshotContextFactory factory = new BookingSnapshotContextFactory();

    @Test
    void buildsLookupMapsAndIgnoresInactiveScheduleExceptionsAndPlaceClosings() {
        BookingContext context = new BookingContext(
                1L,
                1L,
                LocalDateTime.now(BookingServiceUnitFixtures.CLOCK),
                null,
                "Coworking",
                true,
                List.of(BookingServiceUnitFixtures.floor(10L, true)),
                List.of(BookingServiceUnitFixtures.tariff(30L, 1_500L, true)),
                List.of(BookingServiceUnitFixtures.placeType(20L, 30L, true)),
                List.of(BookingServiceUnitFixtures.place(100L, 10L, 20L, true)),
                List.of(
                        new BookingContext.ScheduleException(1L, BookingServiceUnitFixtures.TODAY.plusDays(1), "CLOSE", "Inactive close", false),
                        new BookingContext.ScheduleException(2L, BookingServiceUnitFixtures.TODAY.plusDays(2), "OPEN", "Active open", true)
                ),
                List.of(
                        new BookingContext.PlaceClosing(1L, 100L, BookingServiceUnitFixtures.TODAY.plusDays(3), "Inactive closing", false),
                        new BookingContext.PlaceClosing(2L, 100L, BookingServiceUnitFixtures.TODAY.plusDays(4), "Active closing", true)
                )
        );

        BookingSnapshotContext snapshot = factory.from(context);

        assertThat(snapshot.scheduleBitmask()).isZero();
        assertThat(snapshot.floorsById()).containsKey(10L);
        assertThat(snapshot.tariffsById()).containsKey(30L);
        assertThat(snapshot.placeTypesById()).containsKey(20L);
        assertThat(snapshot.placesById()).containsKey(100L);
        assertThat(snapshot.scheduleExceptionsByDate()).containsOnlyKeys(BookingServiceUnitFixtures.TODAY.plusDays(2));
        assertThat(snapshot.placeClosingsByPlaceAndDate()).containsOnlyKeys(
                BookingSnapshotContextFactory.key(100L, BookingServiceUnitFixtures.TODAY.plusDays(4))
        );
    }
}
