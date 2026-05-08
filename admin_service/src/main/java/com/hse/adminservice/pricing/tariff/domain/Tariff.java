package com.hse.adminservice.pricing.tariff.domain;

import com.hse.adminservice.coworking.domain.Coworking;
import com.hse.adminservice.pricing.discount.domain.TariffDiscountRule;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "tariffs", uniqueConstraints = {@UniqueConstraint(
        name = "uk_tariff_coworking_name_archived", columnNames = {"coworking_id", "name", "archived"}
)}
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tariff {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "coworking_id", nullable = false)
    private Coworking coworking;

    @Column(nullable = false)
    private String name;

    @Column(name = "price_per_day", nullable = false)
    private Integer pricePerDay;

    @Column(name = "min_booking_days", nullable = false)
    private Integer minBookingDays;

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

    @OneToMany(mappedBy = "tariff", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TariffDiscountRule> discountRules = new ArrayList<>();

    @Column(name = "tariff_version", nullable = false)
    private Integer version;

    @Column(nullable = false)
    private Boolean active;

    @Column(nullable = false)
    private Boolean archived;

    private LocalDateTime archivedAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
