package com.hse.userservice.feature.booking.service;

import com.hse.userservice.feature.booking.domain.Booking;
import com.hse.userservice.feature.booking.domain.BookingStatus;
import com.hse.userservice.integration.dto.BookingContext;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class BookingEntityFactoryTest {
    private final BookingEntityFactory factory = new BookingEntityFactory();

    @Test
    void createsActualActiveBookingAndCopiesTariffSnapshotFields() {
        BookingContext.Tariff tariff = new BookingContext.Tariff(
                30L,
                "Day pass",
                1_500L,
                12,
                35,
                new BigDecimal("1.1000"),
                new BigDecimal("1.2500"),
                new BigDecimal("0.7500"),
                3,
                true
        );
        ResolvedCartItem item = new ResolvedCartItem(
                100L,
                "Desk 100",
                BookingServiceUnitFixtures.TODAY.plusDays(1),
                "Floor 1",
                "Desk",
                tariff,
                1_500L,
                true
        );

        Booking booking = factory.fromCartItem(11L, "REQ-UNIT", "BR-2026-000001", item);

        assertThat(booking.getMembershipId()).isEqualTo(11L);
        assertThat(booking.getPlaceId()).isEqualTo(100L);
        assertThat(booking.getDate()).isEqualTo(item.date());
        assertThat(booking.getCost()).isEqualTo(1_500L);
        assertThat(booking.getActive()).isTrue();
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.ACTUAL);
        assertThat(booking.getBookingNumber()).isEqualTo("BR-2026-000001");
        assertThat(booking.getRequestId()).isEqualTo("REQ-UNIT");
        assertThat(booking.getTariffId()).isEqualTo(30L);
        assertThat(booking.getPricePerDay()).isEqualTo(1_500L);
        assertThat(booking.getFullRefundHoursBefore()).isEqualTo(12);
        assertThat(booking.getLateCancellationRefundPercent()).isEqualTo(35);
        assertThat(booking.getCancellationCompensationCoefficient()).isEqualByComparingTo("1.1000");
        assertThat(booking.getDayClosureCompensationCoefficient()).isEqualByComparingTo("1.2500");
        assertThat(booking.getMembershipBlockCompensationCoefficient()).isEqualByComparingTo("0.7500");
    }
}
