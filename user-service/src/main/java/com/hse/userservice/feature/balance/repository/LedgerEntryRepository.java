package com.hse.userservice.feature.balance.repository;

import com.hse.userservice.feature.balance.domain.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {
    @Query(value = "select nextval('manual_adjustment_reference_seq')", nativeQuery = true)
    Long nextManualAdjustmentReferenceId();

    List<LedgerEntry> findAllByMembershipIdOrderByTimestampDesc(Long membershipId);

    List<LedgerEntry> findAllByMembershipIdInOrderByTimestampDesc(Collection<Long> membershipIds);
}
