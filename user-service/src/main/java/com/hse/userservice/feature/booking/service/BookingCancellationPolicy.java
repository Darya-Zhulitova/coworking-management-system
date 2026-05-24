package com.hse.userservice.feature.booking.service;

import com.hse.userservice.feature.booking.domain.Booking;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class BookingCancellationPolicy {
    private final Clock clock;

    public long calculatePreview(Booking booking) {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime bookingStart = booking.getDate().atStartOfDay();
        if (!bookingStart.isAfter(now)) {
            return 0L;
        }

        long hoursUntil = Duration.between(now, bookingStart).toHours();
        if (hoursUntil >= booking.getFullRefundHoursBefore()) {
            return booking.getCost();
        }
        return Math.floorDiv(booking.getCost() * booking.getLateCancellationRefundPercent(), 100);
    }
}
