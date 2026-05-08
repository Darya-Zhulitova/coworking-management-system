package com.hse.adminservice.pricing.tariff.dto;

import com.hse.adminservice.pricing.discount.dto.TariffDiscountRuleRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class TariffCreateRequest {
    @NotBlank
    private String name;
    @NotNull
    @Min(0)
    private Integer pricePerDay;
    @NotNull
    @Min(1)
    private Integer minBookingDays;
    @NotNull
    @Min(0)
    private Integer fullRefundHoursBefore;
    @NotNull
    @Min(0)
    @Max(100)
    private Integer lateCancellationRefundPercent;
    @DecimalMin("0.0")
    private BigDecimal cancellationCompensationCoefficient;
    @DecimalMin("0.0")
    private BigDecimal dayClosureCompensationCoefficient;
    @DecimalMin("0.0")
    private BigDecimal membershipBlockCompensationCoefficient;
    @Valid
    private List<TariffDiscountRuleRequest> discountRules = new ArrayList<>();
}
