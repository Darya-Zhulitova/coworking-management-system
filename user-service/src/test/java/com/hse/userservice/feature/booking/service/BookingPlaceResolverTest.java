package com.hse.userservice.feature.booking.service;

import com.hse.userservice.common.exception.ResourceConflictException;
import com.hse.userservice.common.exception.ResourceNotFoundException;
import com.hse.userservice.integration.dto.BookingContext;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BookingPlaceResolverTest {
    private final BookingSnapshotContextFactory contextFactory = new BookingSnapshotContextFactory();
    private final BookingPlaceResolver resolver = new BookingPlaceResolver();

    @Test
    void resolvesPlaceWithLinkedTypeTariffAndFloor() {
        BookingSnapshotContext context = contextFactory.from(BookingServiceUnitFixtures.bookingContext());

        ResolvedPlace resolved = resolver.resolve(100L, context, 1L);

        assertThat(resolved.coworkingId()).isEqualTo(1L);
        assertThat(resolved.place().id()).isEqualTo(100L);
        assertThat(resolved.placeType().id()).isEqualTo(20L);
        assertThat(resolved.tariff().id()).isEqualTo(30L);
        assertThat(resolved.floor().id()).isEqualTo(10L);
    }

    @Test
    void throwsNotFoundWhenPlaceIsMissing() {
        BookingSnapshotContext context = contextFactory.from(BookingServiceUnitFixtures.bookingContext());

        assertThatThrownBy(() -> resolver.resolve(999L, context, 1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Место не найдено");
    }

    @Test
    void throwsConflictWhenLinkedObjectsAreMissingOrInactive() {
        BookingContext missingTariff = new BookingContext(
                1L,
                1L,
                LocalDateTime.now(BookingServiceUnitFixtures.CLOCK),
                31,
                "Coworking",
                true,
                List.of(BookingServiceUnitFixtures.floor(10L, true)),
                List.of(),
                List.of(BookingServiceUnitFixtures.placeType(20L, 30L, true)),
                List.of(BookingServiceUnitFixtures.place(100L, 10L, 20L, true)),
                List.of(),
                List.of()
        );
        BookingContext inactiveType = new BookingContext(
                1L,
                1L,
                LocalDateTime.now(BookingServiceUnitFixtures.CLOCK),
                31,
                "Coworking",
                true,
                List.of(BookingServiceUnitFixtures.floor(10L, true)),
                List.of(BookingServiceUnitFixtures.tariff(30L, 1_500L, true)),
                List.of(BookingServiceUnitFixtures.placeType(20L, 30L, false)),
                List.of(BookingServiceUnitFixtures.place(100L, 10L, 20L, true)),
                List.of(),
                List.of()
        );

        assertThatThrownBy(() -> resolver.resolve(100L, contextFactory.from(missingTariff), 1L))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("Тариф не найден");
        assertThatThrownBy(() -> resolver.resolve(100L, contextFactory.from(inactiveType), 1L))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("недоступно для бронирования");
    }
}
