package com.hse.userservice.feature.booking.service;

import com.hse.userservice.feature.booking.domain.Booking;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class BookingCompensationCalculator {
    public long calculate(Booking booking) {
        return calculateWithCoefficient(booking, booking.getDayClosureCompensationCoefficient());
    }

    public long calculateMembershipBlockCompensation(Booking booking) {
        return calculateWithCoefficient(booking, booking.getMembershipBlockCompensationCoefficient());
    }

    private long calculateWithCoefficient(Booking booking, BigDecimal coefficient) {
        return BigDecimal.valueOf(booking.getCost()).multiply(coefficient).setScale(0, RoundingMode.DOWN).longValue();
    }
}
