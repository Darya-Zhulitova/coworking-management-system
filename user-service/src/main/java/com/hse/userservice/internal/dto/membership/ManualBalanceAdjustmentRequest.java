package com.hse.userservice.internal.dto.membership;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ManualBalanceAdjustmentRequest(
        @NotNull Long amountMinorUnits,
        @Size(max = 1000) String comment
) {
}
