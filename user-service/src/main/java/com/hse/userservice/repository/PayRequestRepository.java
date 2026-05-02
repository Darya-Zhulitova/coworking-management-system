package com.hse.userservice.repository;

import com.hse.userservice.domain.payrequest.PayRequest;
import com.hse.userservice.domain.payrequest.PayRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PayRequestRepository extends JpaRepository<PayRequest, Long> {
    List<PayRequest> findAllByMembershipIdInOrderByCreatedAtDesc(Collection<Long> membershipIds);

    List<PayRequest> findAllByMembershipIdOrderByCreatedAtDesc(Long membershipId);

    long countByMembershipIdInAndStatus(Collection<Long> membershipIds, PayRequestStatus status);

    Optional<PayRequest> findByIdAndMembershipIdIn(Long id, Collection<Long> membershipIds);
}
