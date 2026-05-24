package com.hse.userservice.support.builder;

import com.hse.userservice.feature.balance.domain.LedgerEntry;
import com.hse.userservice.feature.balance.domain.LedgerEntryType;

import java.time.LocalDateTime;

public class LedgerEntryTestBuilder {
    private Long membershipId = 1L;
    private LedgerEntryType type = LedgerEntryType.BALANCE_TOP_UP;
    private Long amount = 5000L;
    private Long referenceId = 1L;
    private String comment = "Test ledger entry";
    private LocalDateTime timestamp = LocalDateTime.of(2026, 1, 1, 11, 0);

    public static LedgerEntryTestBuilder ledgerEntry() { return new LedgerEntryTestBuilder(); }

    public LedgerEntryTestBuilder membershipId(Long membershipId) { this.membershipId = membershipId; return this; }
    public LedgerEntryTestBuilder type(LedgerEntryType type) { this.type = type; return this; }
    public LedgerEntryTestBuilder amount(Long amount) { this.amount = amount; return this; }
    public LedgerEntryTestBuilder referenceId(Long referenceId) { this.referenceId = referenceId; return this; }
    public LedgerEntryTestBuilder comment(String comment) { this.comment = comment; return this; }
    public LedgerEntryTestBuilder timestamp(LocalDateTime timestamp) { this.timestamp = timestamp; return this; }

    public LedgerEntry build() {
        LedgerEntry entry = new LedgerEntry();
        entry.setMembershipId(membershipId);
        entry.setType(type);
        entry.setAmount(amount);
        entry.setReferenceId(referenceId);
        entry.setComment(comment);
        entry.setTimestamp(timestamp);
        entry.setName(type.getDisplayName());
        return entry;
    }
}
