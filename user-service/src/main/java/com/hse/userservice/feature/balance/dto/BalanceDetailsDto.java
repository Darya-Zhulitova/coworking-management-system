package com.hse.userservice.feature.balance.dto;

import com.hse.userservice.feature.membership.domain.MembershipStatus;

import java.util.List;

public record BalanceDetailsDto(
        Long membershipId,
        MembershipStatus membershipStatus,
        Long balanceMinorUnits,
        List<LedgerEntryDto> ledger,
        List<PayRequestDto> payRequests
) {
}
