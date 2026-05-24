package com.hse.userservice.feature.balance.service;

import com.hse.userservice.feature.balance.domain.LedgerEntry;
import com.hse.userservice.feature.balance.domain.LedgerEntryType;
import com.hse.userservice.feature.balance.repository.LedgerEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class LedgerService {
    private final LedgerEntryRepository ledgerEntryRepository;
    private final Clock clock;

    @Transactional
    public LedgerEntry createPayRequestEntry(Long membershipId, long amount, Long payRequestId, String comment) {
        LedgerEntryType type = amount >= 0 ? LedgerEntryType.BALANCE_TOP_UP : LedgerEntryType.BALANCE_WITHDRAWAL;
        return create(membershipId, amount, type, payRequestId, comment);
    }

    @Transactional
    public LedgerEntry createManualAdjustment(Long membershipId, long amount, String comment) {
        LedgerEntryType type = amount > 0 ? LedgerEntryType.MANUAL_CREDIT : LedgerEntryType.MANUAL_DEBIT;
        String normalizedComment = StringUtils.hasText(comment) ? comment.trim() : "Корректировка администратором";
        return create(membershipId, amount, type, nextManualReferenceId(), normalizedComment);
    }

    @Transactional
    public LedgerEntry createServiceRequestCharge(Long membershipId, long cost, Long serviceRequestId, String comment) {
        return create(membershipId, -cost, LedgerEntryType.SERVICE_REQUEST_CHARGE, serviceRequestId, comment);
    }

    @Transactional
    public LedgerEntry createAdminBookingCompensation(
            Long membershipId,
            long compensation,
            Long bookingId,
            String comment
    ) {
        return create(
                membershipId,
                compensation,
                LedgerEntryType.BOOKING_ADMIN_CANCELLATION_COMPENSATION,
                bookingId,
                comment
        );
    }

    private LedgerEntry create(Long membershipId, long amount, LedgerEntryType type, Long referenceId, String comment) {
        LedgerEntry entry = new LedgerEntry();
        entry.setMembershipId(membershipId);
        entry.setAmount(amount);
        entry.setType(type);
        entry.setComment(comment);
        entry.setReferenceId(referenceId);
        entry.setTimestamp(LocalDateTime.now(clock));
        return ledgerEntryRepository.saveAndFlush(entry);
    }

    private Long nextManualReferenceId() {
        return -ledgerEntryRepository.nextManualAdjustmentReferenceId();
    }
}
