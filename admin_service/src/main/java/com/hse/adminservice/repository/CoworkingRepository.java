package com.hse.adminservice.repository;

import com.hse.adminservice.entity.Coworking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CoworkingRepository extends JpaRepository<Coworking, Long> {

    List<Coworking> findAllByArchivedFalse();

    List<Coworking> findAllByActiveTrueAndArchivedFalse();

    Optional<Coworking> findByIdAndArchivedFalse(Long id);
}