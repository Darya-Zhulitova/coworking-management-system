package com.hse.userservice.feature.booking.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(
        name = "bookings", indexes = {@Index(
        name = "idx_bookings_membership_date", columnList = "membership_id,date"
), @Index(name = "idx_bookings_place_date", columnList = "place_id,date")}
)
@Getter
@Setter
@NoArgsConstructor
public class Booking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    @Column(name = "place_id", nullable = false)
    private Long placeId;

    @Column(name = "membership_id", nullable = false)
    private Long membershipId;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private Long cost;

    @Column(nullable = false)
    private Boolean active = true;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private BookingStatus status = BookingStatus.ACTUAL;

    @Column(name = "booking_number", nullable = false, unique = true, length = 32)
    private String bookingNumber;

    @Column(name = "request_id", nullable = false, length = 64)
    private String requestId;

    @Column(name = "tariff_id", nullable = false)
    private Long tariffId;

    @Column(name = "price_per_day", nullable = false)
    private Long pricePerDay;

    @Column(name = "full_refund_hours_before", nullable = false)
    private Integer fullRefundHoursBefore;

    @Column(name = "late_cancellation_refund_percent", nullable = false)
    private Integer lateCancellationRefundPercent;

    @Column(name = "cancellation_compensation_coefficient", nullable = false, precision = 10, scale = 4)
    private BigDecimal cancellationCompensationCoefficient;

    @Column(name = "day_closure_compensation_coefficient", nullable = false, precision = 10, scale = 4)
    private BigDecimal dayClosureCompensationCoefficient;

    @Column(name = "membership_block_compensation_coefficient", nullable = false, precision = 10, scale = 4)
    private BigDecimal membershipBlockCompensationCoefficient;
}
