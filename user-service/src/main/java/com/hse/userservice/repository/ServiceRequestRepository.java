package com.hse.userservice.repository;

import com.hse.userservice.domain.request.ServiceRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ServiceRequestRepository extends JpaRepository<ServiceRequest, Long> {
    List<ServiceRequest> findByMembershipIdOrderByCreatedAtDesc(Long membershipId);
}