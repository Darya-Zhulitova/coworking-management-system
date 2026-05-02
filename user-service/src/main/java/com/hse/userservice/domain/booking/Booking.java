package com.hse.userservice.domain.booking;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(
        name = "bookings", uniqueConstraints = {@UniqueConstraint(
        name = "uk_booking_place_date_active", columnNames = {"place_id", "date", "active"}
)}
)
@Getter
@Setter
@NoArgsConstructor
public class Booking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "coworking_id", nullable = false)
    private Long coworkingId;

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

    @Column(name = "request_id", nullable = false, length = 64)
    private String requestId;

    @Column(name = "tariff_id", nullable = false)
    private Long tariffId;

    @Column(name = "price_per_day", nullable = false)
    private Integer pricePerDay;

    @Column(name = "applied_discount_percent", nullable = false)
    private Integer appliedDiscountPercent;

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
