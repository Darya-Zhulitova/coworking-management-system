package com.hse.userservice.repository;

import com.hse.userservice.domain.membership.Membership;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MembershipRepository extends JpaRepository<Membership, Long> {
}