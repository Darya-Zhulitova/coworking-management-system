package com.hse.userservice.feature.membership.repository;

import com.hse.userservice.feature.membership.domain.Membership;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MembershipRepository extends JpaRepository<Membership, Long> {
    Optional<Membership> findByUserIdAndCoworkingId(Long userId, Long coworkingId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Membership m where m.id = :id")
    Optional<Membership> findByIdForUpdate(@Param("id") Long id);

    List<Membership> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    List<Membership> findAllByCoworkingIdOrderByCreatedAtDesc(Long coworkingId);
}
