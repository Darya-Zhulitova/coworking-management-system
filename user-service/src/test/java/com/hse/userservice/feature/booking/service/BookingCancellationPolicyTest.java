package com.hse.userservice.feature.booking.service;

import com.hse.userservice.feature.booking.domain.Booking;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BookingCancellationPolicyTest {
    private final BookingCancellationPolicy policy = new BookingCancellationPolicy(BookingServiceUnitFixtures.CLOCK);

    @Test
    void returnsFullRefundWhenBookingStartsAfterConfiguredFullRefundWindow() {
        Booking booking = BookingServiceUnitFixtures.booking(1L, 11L, 100L, BookingServiceUnitFixtures.TODAY.plusDays(3), 2_000L);
        booking.setFullRefundHoursBefore(48);

        long refund = policy.calculatePreview(booking);

        assertThat(refund).isEqualTo(2_000L);
    }

    @Test
    void returnsLateCancellationPercentWhenBookingStartsInsideFullRefundWindow() {
        Booking booking = BookingServiceUnitFixtures.booking(2L, 11L, 100L, BookingServiceUnitFixtures.TODAY.plusDays(1), 2_500L);
        booking.setFullRefundHoursBefore(48);
        booking.setLateCancellationRefundPercent(40);

        long refund = policy.calculatePreview(booking);

        assertThat(refund).isEqualTo(1_000L);
    }

    @Test
    void returnsZeroWhenBookingHasAlreadyStartedOrStartsToday() {
        Booking todayBooking = BookingServiceUnitFixtures.booking(3L, 11L, 100L, BookingServiceUnitFixtures.TODAY, 2_000L);
        Booking pastBooking = BookingServiceUnitFixtures.booking(4L, 11L, 100L, BookingServiceUnitFixtures.TODAY.minusDays(1), 2_000L);

        assertThat(policy.calculatePreview(todayBooking)).isZero();
        assertThat(policy.calculatePreview(pastBooking)).isZero();
    }
}
