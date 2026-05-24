package com.hse.userservice.feature.balance.dto;

import com.hse.userservice.feature.balance.domain.LedgerEntryType;

import java.time.LocalDateTime;

public record LedgerEntryDto(
        Long id,
        LocalDateTime timestamp,
        LedgerEntryType type,
        String name,
        String comment,
        Long amount
) {
}
