package com.hse.adminservice.operations.common;

import jakarta.validation.constraints.NotNull;

public record DecisionRequest(
        @NotNull Decision decision,
        String comment
) {
    public enum Decision {
        APPROVE, REJECT, IN_PROGRESS, RESOLVE
    }
}
