package com.hse.userservice.repository;

import com.hse.userservice.domain.ledger.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {
    List<LedgerEntry> findAllByMembershipIdOrderByTimestampDesc(Long membershipId);

    List<LedgerEntry> findAllByCoworkingIdOrderByTimestampDesc(Long coworkingId);

    List<LedgerEntry> findAllByCoworkingIdAndTimestampBetweenOrderByTimestampAsc(
            Long coworkingId,
            LocalDateTime from,
            LocalDateTime to
    );

    List<LedgerEntry> findAllByMembershipIdInOrderByTimestampDesc(Collection<Long> membershipIds);
}
