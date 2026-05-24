package com.hse.userservice.feature.balance.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "ledger_entries", uniqueConstraints = {@UniqueConstraint(
        name = "uk_ledger_membership_type_reference", columnNames = {"membership_id", "type", "reference_id"}
)}, indexes = {@Index(name = "idx_ledger_membership_timestamp", columnList = "membership_id,timestamp")}
)
@Getter
@Setter
@NoArgsConstructor
public class LedgerEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "membership_id", nullable = false)
    private Long membershipId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 64)
    private LedgerEntryType type;

    @Column(nullable = false)
    private Long amount;

    @Column(nullable = false)
    private String name;

    @Column(length = 1000)
    private String comment;

    @Column(name = "reference_id", nullable = false)
    private Long referenceId;

    @Column(nullable = false)
    private LocalDateTime timestamp = LocalDateTime.now();

    @PrePersist
    @PreUpdate
    private void syncNameWithType() {
        if (type != null) {
            name = type.getDisplayName();
        }
    }
}
