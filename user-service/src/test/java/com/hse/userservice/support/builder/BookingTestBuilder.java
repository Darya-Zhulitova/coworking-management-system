package com.hse.userservice.support.builder;

import com.hse.userservice.feature.booking.domain.Booking;
import com.hse.userservice.feature.booking.domain.BookingStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public class BookingTestBuilder {
    private Long placeId = 1L;
    private Long membershipId = 1L;
    private LocalDate date = LocalDate.of(2026, 1, 10);
    private Long cost = 1000L;
    private Boolean active = true;
    private BookingStatus status = BookingStatus.ACTUAL;
    private String bookingNumber = "B-TEST-" + UUID.randomUUID().toString().substring(0, 8);
    private String requestId = UUID.randomUUID().toString();
    private Long tariffId = 1L;
    private Long pricePerDay = 1000L;
    private Integer fullRefundHoursBefore = 24;
    private Integer lateCancellationRefundPercent = 50;
    private BigDecimal cancellationCompensationCoefficient = new BigDecimal("1.0000");
    private BigDecimal dayClosureCompensationCoefficient = new BigDecimal("1.0000");
    private BigDecimal membershipBlockCompensationCoefficient = new BigDecimal("1.0000");

    public static BookingTestBuilder booking() {
        return new BookingTestBuilder();
    }

    public BookingTestBuilder placeId(Long placeId) { this.placeId = placeId; return this; }
    public BookingTestBuilder membershipId(Long membershipId) { this.membershipId = membershipId; return this; }
    public BookingTestBuilder date(LocalDate date) { this.date = date; return this; }
    public BookingTestBuilder cost(Long cost) { this.cost = cost; return this; }
    public BookingTestBuilder active(Boolean active) { this.active = active; return this; }
    public BookingTestBuilder status(BookingStatus status) { this.status = status; return this; }
    public BookingTestBuilder bookingNumber(String bookingNumber) { this.bookingNumber = bookingNumber; return this; }
    public BookingTestBuilder requestId(String requestId) { this.requestId = requestId; return this; }
    public BookingTestBuilder tariffId(Long tariffId) { this.tariffId = tariffId; return this; }
    public BookingTestBuilder pricePerDay(Long pricePerDay) { this.pricePerDay = pricePerDay; return this; }
    public BookingTestBuilder fullRefundHoursBefore(Integer fullRefundHoursBefore) { this.fullRefundHoursBefore = fullRefundHoursBefore; return this; }
    public BookingTestBuilder lateCancellationRefundPercent(Integer lateCancellationRefundPercent) { this.lateCancellationRefundPercent = lateCancellationRefundPercent; return this; }

    public Booking build() {
        Booking booking = new Booking();
        booking.setPlaceId(placeId);
        booking.setMembershipId(membershipId);
        booking.setDate(date);
        booking.setCost(cost);
        booking.setActive(active);
        booking.setStatus(status);
        booking.setBookingNumber(bookingNumber);
        booking.setRequestId(requestId);
        booking.setTariffId(tariffId);
        booking.setPricePerDay(pricePerDay);
        booking.setFullRefundHoursBefore(fullRefundHoursBefore);
        booking.setLateCancellationRefundPercent(lateCancellationRefundPercent);
        booking.setCancellationCompensationCoefficient(cancellationCompensationCoefficient);
        booking.setDayClosureCompensationCoefficient(dayClosureCompensationCoefficient);
        booking.setMembershipBlockCompensationCoefficient(membershipBlockCompensationCoefficient);
        return booking;
    }
}
