package com.hse.userservice.feature.booking.service;

import com.hse.userservice.feature.booking.domain.Booking;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class BookingCompensationCalculatorTest {
    private final BookingCompensationCalculator calculator = new BookingCompensationCalculator();

    @Test
    void multipliesBookingCostByDayClosureCoefficientAndRoundsDown() {
        Booking booking = BookingServiceUnitFixtures.booking(1L, 11L, 100L, BookingServiceUnitFixtures.TODAY.plusDays(2), 1_999L);
        booking.setDayClosureCompensationCoefficient(new BigDecimal("1.2550"));

        long compensation = calculator.calculate(booking);

        assertThat(compensation).isEqualTo(2_508L);
    }
}
