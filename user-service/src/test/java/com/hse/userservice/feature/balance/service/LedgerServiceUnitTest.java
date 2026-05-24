package com.hse.userservice.feature.balance.service;

import com.hse.userservice.feature.balance.domain.LedgerEntry;
import com.hse.userservice.feature.balance.domain.LedgerEntryType;
import com.hse.userservice.feature.balance.repository.LedgerEntryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LedgerServiceUnitTest {
    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-05-22T10:15:30Z"), ZoneOffset.UTC);
    private static final LocalDateTime NOW = LocalDateTime.ofInstant(FIXED_CLOCK.instant(), FIXED_CLOCK.getZone());

    @Mock LedgerEntryRepository ledgerEntryRepository;

    @Test
    void createPayRequestEntryCreatesTopUpForPositiveAmount() {
        LedgerService service = service();
        when(ledgerEntryRepository.saveAndFlush(any(LedgerEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LedgerEntry entry = service.createPayRequestEntry(11L, 5_000L, 77L, " receipt ");

        assertThat(entry.getMembershipId()).isEqualTo(11L);
        assertThat(entry.getAmount()).isEqualTo(5_000L);
        assertThat(entry.getType()).isEqualTo(LedgerEntryType.BALANCE_TOP_UP);
        assertThat(entry.getReferenceId()).isEqualTo(77L);
        assertThat(entry.getComment()).isEqualTo(" receipt ");
        assertThat(entry.getTimestamp()).isEqualTo(NOW);
    }

    @Test
    void createPayRequestEntryCreatesWithdrawalForNegativeAmount() {
        LedgerService service = service();
        when(ledgerEntryRepository.saveAndFlush(any(LedgerEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LedgerEntry entry = service.createPayRequestEntry(11L, -2_000L, 78L, "withdraw");

        assertThat(entry.getType()).isEqualTo(LedgerEntryType.BALANCE_WITHDRAWAL);
        assertThat(entry.getAmount()).isEqualTo(-2_000L);
        assertThat(entry.getReferenceId()).isEqualTo(78L);
    }

    @Test
    void createManualAdjustmentUsesCreditDebitTypesDefaultCommentAndNegativeSequenceReference() {
        LedgerService service = service();
        when(ledgerEntryRepository.nextManualAdjustmentReferenceId()).thenReturn(123L);
        when(ledgerEntryRepository.saveAndFlush(any(LedgerEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LedgerEntry credit = service.createManualAdjustment(11L, 100L, "  bonus  ");
        LedgerEntry debit = service.createManualAdjustment(11L, -50L, " ");

        assertThat(credit.getType()).isEqualTo(LedgerEntryType.MANUAL_CREDIT);
        assertThat(credit.getComment()).isEqualTo("bonus");
        assertThat(credit.getReferenceId()).isEqualTo(-123L);
        assertThat(debit.getType()).isEqualTo(LedgerEntryType.MANUAL_DEBIT);
        assertThat(debit.getComment()).isEqualTo("Корректировка администратором");
        assertThat(debit.getReferenceId()).isEqualTo(-123L);
    }

    @Test
    void createServiceRequestChargeCreatesNegativeLedgerEntry() {
        LedgerService service = service();
        when(ledgerEntryRepository.saveAndFlush(any(LedgerEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LedgerEntry entry = service.createServiceRequestCharge(11L, 900L, 45L, "coffee");

        assertThat(entry.getMembershipId()).isEqualTo(11L);
        assertThat(entry.getAmount()).isEqualTo(-900L);
        assertThat(entry.getType()).isEqualTo(LedgerEntryType.SERVICE_REQUEST_CHARGE);
        assertThat(entry.getReferenceId()).isEqualTo(45L);
        assertThat(entry.getComment()).isEqualTo("coffee");
    }

    @Test
    void createAdminBookingCompensationCreatesPositiveCompensationEntry() {
        LedgerService service = service();
        when(ledgerEntryRepository.saveAndFlush(any(LedgerEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.createAdminBookingCompensation(11L, 1_250L, 99L, "closed day");

        ArgumentCaptor<LedgerEntry> captor = ArgumentCaptor.forClass(LedgerEntry.class);
        verify(ledgerEntryRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getAmount()).isEqualTo(1_250L);
        assertThat(captor.getValue().getType()).isEqualTo(LedgerEntryType.BOOKING_ADMIN_CANCELLATION_COMPENSATION);
        assertThat(captor.getValue().getReferenceId()).isEqualTo(99L);
        assertThat(captor.getValue().getComment()).isEqualTo("closed day");
    }

    private LedgerService service() {
        return new LedgerService(ledgerEntryRepository, FIXED_CLOCK);
    }
}
