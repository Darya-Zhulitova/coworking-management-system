package com.hse.adminservice.operations.membership.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ManualBalanceAdjustmentRequest(
        @NotNull Long amountMinorUnits,
        @Size(max = 1000) String comment
) {
}
