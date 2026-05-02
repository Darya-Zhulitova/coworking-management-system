package com.hse.userservice.dto.response;

import com.hse.userservice.domain.ledger.LedgerEntryType;

import java.time.LocalDateTime;

public record LedgerEntryDto(
        Long id,
        Long coworkingId,
        LocalDateTime timestamp,
        LedgerEntryType type,
        String name,
        String comment,
        Long amount
) {
}
