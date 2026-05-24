package com.hse.userservice.internal.service;

import com.hse.userservice.feature.booking.domain.Booking;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DeactivationImpactHashServiceTest {
    private final DeactivationImpactHashService service = new DeactivationImpactHashService();

    @Test
    void createHashReturnsStableSha256HashForSameInput() {
        List<Booking> bookings = List.of(booking(2L), booking(1L));

        String first = service.createHash("DAY_CLOSING", 7L, null, List.of(LocalDate.parse("2026-05-25")), bookings);
        String second = service.createHash("DAY_CLOSING", 7L, null, List.of(LocalDate.parse("2026-05-25")), bookings);

        assertThat(first).startsWith("sha256:");
        assertThat(second).isEqualTo(first);
    }

    @Test
    void createHashDoesNotDependOnBookingListOrder() {
        Booking first = booking(1L);
        Booking second = booking(2L);

        String original = service.createHash("DAY_CLOSING", 7L, null, List.of(), List.of(first, second));
        String reordered = service.createHash("DAY_CLOSING", 7L, null, List.of(), List.of(second, first));

        assertThat(reordered).isEqualTo(original);
    }

    @Test
    void createHashChangesWhenOperationDatesOrBookingStateChanges() {
        Booking booking = booking(1L);
        String base = service.createHash("DAY_CLOSING", 7L, null, List.of(LocalDate.parse("2026-05-25")), List.of(booking));

        String differentOperation = service.createHash(
                "SCHEDULE_REDUCTION",
                7L,
                null,
                List.of(LocalDate.parse("2026-05-25")),
                List.of(booking)
        );
        String differentDate = service.createHash("DAY_CLOSING", 7L, null, List.of(LocalDate.parse("2026-05-26")), List.of(booking));
        booking.setCost(2_000L);
        String differentCost = service.createHash("DAY_CLOSING", 7L, null, List.of(LocalDate.parse("2026-05-25")), List.of(booking));

        assertThat(differentOperation).isNotEqualTo(base);
        assertThat(differentDate).isNotEqualTo(base);
        assertThat(differentCost).isNotEqualTo(base);
    }

    @Test
    void createHashUsesBookingDatesWhenRequestedDatesAreMissing() {
        Booking booking = booking(1L);

        String withNullDates = service.createHash("DAY_CLOSING", 7L, null, null, List.of(booking));
        String withEmptyDates = service.createHash("DAY_CLOSING", 7L, null, List.of(), List.of(booking));

        assertThat(withEmptyDates).isEqualTo(withNullDates);
    }

    private static Booking booking(Long id) {
        Booking booking = new Booking();
        booking.setId(id);
        booking.setMembershipId(11L);
        booking.setPlaceId(100L);
        booking.setDate(LocalDate.parse("2026-05-25").plusDays(id));
        booking.setCost(1_000L);
        booking.setDayClosureCompensationCoefficient(new java.math.BigDecimal("1.2500"));
        return booking;
    }
}
