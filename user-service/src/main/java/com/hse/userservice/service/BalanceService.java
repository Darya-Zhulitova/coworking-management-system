package com.hse.userservice.service;

import com.hse.userservice.domain.ledger.LedgerEntry;
import com.hse.userservice.repository.LedgerEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BalanceService {
    private final LedgerEntryRepository ledgerEntryRepository;

    public long getBalanceMinorUnits(Long membershipId) {
        return ledgerEntryRepository.findAllByMembershipIdOrderByTimestampDesc(membershipId).stream().mapToLong(
                LedgerEntry::getAmount).sum();
    }

    public Map<Long, Long> getBalancesMinorUnits(Collection<Long> membershipIds) {
        return ledgerEntryRepository.findAllByMembershipIdInOrderByTimestampDesc(membershipIds).stream().collect(
                Collectors.groupingBy(LedgerEntry::getMembershipId, Collectors.summingLong(LedgerEntry::getAmount)));
    }

    public BigDecimal toMajorUnits(long minorUnits) {
        return BigDecimal.valueOf(minorUnits, 2);
    }
}
