package com.hse.userservice.internal.service;

import com.hse.userservice.feature.booking.domain.Booking;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class DeactivationImpactHashService {
    public String createHash(
            String operation,
            Long coworkingId,
            Long placeId,
            List<LocalDate> requestedDates,
            List<Booking> bookings
    ) {
        String canonicalPayload = "operation=" + nullSafe(operation) + "|coworkingId=" + coworkingId + "|placeId=" + nullSafe(
                placeId) + "|affectedDates=" + affectedDates(
                requestedDates,
                bookings
        ) + "|bookings=" + affectedBookings(bookings);
        return "sha256:" + sha256(canonicalPayload);
    }

    private String affectedDates(List<LocalDate> requestedDates, List<Booking> bookings) {
        List<LocalDate> dates = requestedDates == null || requestedDates.isEmpty() ? bookings.stream()
                .map(Booking::getDate)
                .distinct()
                .toList() : requestedDates;
        return dates.stream().sorted().map(LocalDate::toString).collect(Collectors.joining(",", "[", "]"));
    }

    private String affectedBookings(List<Booking> bookings) {
        return bookings.stream().sorted(Comparator.comparing(Booking::getId)).map(this::bookingPayload).collect(
                Collectors.joining(",", "[", "]"));
    }

    private String bookingPayload(Booking booking) {
        long compensation = BigDecimal.valueOf(booking.getCost())
                .multiply(booking.getDayClosureCompensationCoefficient())
                .setScale(0, RoundingMode.DOWN)
                .longValue();
        return "{" + "bookingId=" + booking.getId() + ";membershipId=" + booking.getMembershipId() + ";placeId=" + booking.getPlaceId() + ";date=" + booking.getDate() + ";cost=" + booking.getCost() + ";compensation=" + compensation + "}";
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            log.error("SHA-256 is not available", exception);
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private String nullSafe(Object value) {
        return value == null ? "" : value.toString();
    }
}
