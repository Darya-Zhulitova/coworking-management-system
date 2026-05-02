package com.hse.userservice.dto.response;

import com.hse.userservice.domain.membership.MembershipStatus;

import java.util.List;

public record BalanceDetailsDto(
        Long membershipId,
        Long coworkingId,
        MembershipStatus membershipStatus,
        Long balanceMinorUnits,
        List<LedgerEntryDto> ledger,
        List<PayRequestDto> payRequests
) {
}
