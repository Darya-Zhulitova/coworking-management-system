package com.hse.userservice.internal.dto;

import jakarta.validation.constraints.NotNull;

public record InternalDecisionRequest(
        @NotNull Decision decision,
        String comment
) {
    public enum Decision {
        APPROVE, REJECT, IN_PROGRESS, RESOLVE
    }
}
