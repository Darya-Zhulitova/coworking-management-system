package com.hse.userservice.repository;

import com.hse.userservice.domain.request.ServiceRequest;
import com.hse.userservice.domain.request.ServiceRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ServiceRequestRepository extends JpaRepository<ServiceRequest, Long> {
    List<ServiceRequest> findByMembershipIdOrderByCreatedAtDesc(Long membershipId);

    List<ServiceRequest> findAllByMembershipIdInOrderByCreatedAtDesc(Collection<Long> membershipIds);

    long countByMembershipIdInAndStatusNotIn(Collection<Long> membershipIds, Collection<ServiceRequestStatus> statuses);

    Optional<ServiceRequest> findByIdAndMembershipIdIn(Long id, Collection<Long> membershipIds);
}
