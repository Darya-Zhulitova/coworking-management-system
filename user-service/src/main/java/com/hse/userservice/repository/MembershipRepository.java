package com.hse.userservice.repository;

import com.hse.userservice.domain.membership.Membership;
import com.hse.userservice.domain.membership.MembershipStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MembershipRepository extends JpaRepository<Membership, Long> {
    Optional<Membership> findByUserIdAndCoworkingId(Long userId, Long coworkingId);

    List<Membership> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<Membership> findByIdAndUserId(Long id, Long userId);

    List<Membership> findAllByCoworkingIdOrderByCreatedAtDesc(Long coworkingId);

    List<Membership> findAllByCoworkingIdAndIdIn(Long coworkingId, Collection<Long> ids);

    long countByCoworkingIdAndStatus(Long coworkingId, MembershipStatus status);
}
