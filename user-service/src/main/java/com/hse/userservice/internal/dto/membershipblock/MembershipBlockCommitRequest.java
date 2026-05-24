package com.hse.userservice.internal.dto.membershipblock;

import jakarta.validation.constraints.NotBlank;

public record MembershipBlockCommitRequest(@NotBlank String impactHash) {
}
