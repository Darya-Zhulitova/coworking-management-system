package com.hse.adminservice.pricing.tariff.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TariffCreateRequest {
    @NotBlank
    private String name;
    @NotNull
    @Min(0)
    private Long pricePerDay;
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
}
