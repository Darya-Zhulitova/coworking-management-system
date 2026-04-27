package com.hse.adminservice.pricing.discount.domain;

import com.hse.adminservice.pricing.tariff.domain.Tariff;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tariff_discount_rules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TariffDiscountRule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tariff_id", nullable = false)
    private Tariff tariff;

    @Column(name = "threshold_quantity", nullable = false)
    private Integer thresholdQuantity;

    @Column(name = "discount_percent", nullable = false)
    private Integer discountPercent;
}
