package com.hse.userservice.domain.ledger;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "ledger_entries")
@Getter
@Setter
@NoArgsConstructor
public class LedgerEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "membership_id", nullable = false)
    private Long membershipId;

    @Column(name = "coworking_id", nullable = false)
    private Long coworkingId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 64)
    private LedgerEntryType type;

    @Column(nullable = false)
    private Long amount;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(length = 1000)
    private String comment;

    @Column(nullable = false)
    private LocalDateTime timestamp = LocalDateTime.now();
}
