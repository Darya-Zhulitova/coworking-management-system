package com.hse.userservice.feature.balance.service;

import com.hse.userservice.feature.balance.domain.LedgerEntry;
import com.hse.userservice.feature.balance.repository.LedgerEntryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UnitsServiceUnitTest {
    @Mock LedgerEntryRepository ledgerEntryRepository;

    @Test
    void getBalanceMinorUnitsSumsAllLedgerEntriesForMembership() {
        when(ledgerEntryRepository.findAllByMembershipIdOrderByTimestampDesc(11L)).thenReturn(List.of(
                entry(11L, 5_000L),
                entry(11L, -1_500L),
                entry(11L, 250L)
        ));

        long balance = new UnitsService(ledgerEntryRepository).getBalanceMinorUnits(11L);

        assertThat(balance).isEqualTo(3_750L);
    }

    @Test
    void getBalanceMinorUnitsReturnsZeroWhenMembershipHasNoLedgerEntries() {
        when(ledgerEntryRepository.findAllByMembershipIdOrderByTimestampDesc(11L)).thenReturn(List.of());

        long balance = new UnitsService(ledgerEntryRepository).getBalanceMinorUnits(11L);

        assertThat(balance).isZero();
    }

    @Test
    void getBalancesMinorUnitsGroupsEntriesByMembershipId() {
        when(ledgerEntryRepository.findAllByMembershipIdInOrderByTimestampDesc(List.of(11L, 12L, 13L))).thenReturn(List.of(
                entry(11L, 1_000L),
                entry(11L, -250L),
                entry(12L, 500L)
        ));

        Map<Long, Long> balances = new UnitsService(ledgerEntryRepository).getBalancesMinorUnits(List.of(11L, 12L, 13L));

        assertThat(balances).containsEntry(11L, 750L).containsEntry(12L, 500L);
        assertThat(balances).doesNotContainKey(13L);
    }

    @Test
    void toMajorUnitsConvertsMinorUnitsToDecimalWithTwoFractionDigits() {
        BigDecimal major = new UnitsService(ledgerEntryRepository).toMajorUnits(12_345L);

        assertThat(major).isEqualByComparingTo("123.45");
    }

    private static LedgerEntry entry(Long membershipId, Long amount) {
        LedgerEntry entry = new LedgerEntry();
        entry.setMembershipId(membershipId);
        entry.setAmount(amount);
        return entry;
    }
}
