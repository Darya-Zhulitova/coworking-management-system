package com.hse.userservice.feature.booking.service;

import com.hse.userservice.feature.booking.domain.Booking;
import com.hse.userservice.feature.booking.domain.BookingStatus;
import com.hse.userservice.feature.membership.domain.Membership;
import com.hse.userservice.feature.membership.domain.MembershipStatus;
import com.hse.userservice.integration.dto.BookingContext;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

final class BookingServiceUnitFixtures {
    static final ZoneId ZONE = ZoneId.of("UTC");
    static final Clock CLOCK = Clock.fixed(Instant.parse("2026-05-22T10:00:00Z"), ZONE);
    static final LocalDate TODAY = LocalDate.now(CLOCK);

    private BookingServiceUnitFixtures() {
    }

    static Booking booking(Long id, Long membershipId, Long placeId, LocalDate date, long cost) {
        Booking booking = new Booking();
        booking.setId(id);
        booking.setMembershipId(membershipId);
        booking.setPlaceId(placeId);
        booking.setDate(date);
        booking.setCost(cost);
        booking.setActive(true);
        booking.setStatus(BookingStatus.ACTUAL);
        booking.setBookingNumber("BR-2026-%06d".formatted(id));
        booking.setRequestId("REQ-%012d".formatted(id));
        booking.setTariffId(300L);
        booking.setPricePerDay(cost);
        booking.setFullRefundHoursBefore(24);
        booking.setLateCancellationRefundPercent(40);
        booking.setCancellationCompensationCoefficient(new BigDecimal("1.0000"));
        booking.setDayClosureCompensationCoefficient(new BigDecimal("1.2500"));
        booking.setMembershipBlockCompensationCoefficient(new BigDecimal("0.5000"));
        return booking;
    }

    static Membership activeMembership(Long id, Long coworkingId) {
        Membership membership = new Membership();
        membership.setId(id);
        membership.setUserId(10L);
        membership.setCoworkingId(coworkingId);
        membership.setStatus(MembershipStatus.ACTIVE);
        membership.setCreatedAt(LocalDateTime.now(CLOCK));
        membership.setApprovedAt(LocalDateTime.now(CLOCK));
        return membership;
    }

    static BookingContext bookingContext() {
        return new BookingContext(
                1L,
                7L,
                LocalDateTime.now(CLOCK),
                31,
                "Unit Coworking",
                true,
                List.of(floor(10L, true)),
                List.of(tariff(30L, 1_500L, true)),
                List.of(placeType(20L, 30L, true)),
                List.of(place(100L, 10L, 20L, true)),
                List.of(),
                List.of()
        );
    }

    static BookingContext.Floor floor(Long id, Boolean active) {
        return new BookingContext.Floor(id, "Floor " + id, 1, "floor-file", "https://example.test/floor.png", active);
    }

    static BookingContext.Tariff tariff(Long id, Long pricePerDay, Boolean active) {
        return new BookingContext.Tariff(
                id,
                "Tariff " + id,
                pricePerDay,
                24,
                50,
                new BigDecimal("1.0000"),
                new BigDecimal("1.2500"),
                new BigDecimal("0.7500"),
                1,
                active
        );
    }

    static BookingContext.PlaceType placeType(Long id, Long tariffId, Boolean active) {
        return new BookingContext.PlaceType(id, "Desk", tariffId, active);
    }

    static BookingContext.Place place(Long id, Long floorId, Long placeTypeId, Boolean active) {
        return new BookingContext.Place(
                id,
                "Place " + id,
                floorId,
                placeTypeId,
                new BigDecimal("12.50"),
                new BigDecimal("25.00"),
                "place-file",
                "https://example.test/place-preview.png",
                "https://example.test/place.png",
                List.of("monitor", "coffee"),
                active
        );
    }
}
