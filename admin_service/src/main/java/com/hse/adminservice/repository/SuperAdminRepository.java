package com.hse.adminservice.repository;

import com.hse.adminservice.entity.SuperAdmin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SuperAdminRepository extends JpaRepository<SuperAdmin, Long> {
    Optional<SuperAdmin> findByEmailAndArchivedFalse(String email);
}
