package com.hse.adminservice.pricing.tariff.persistence;

import com.hse.adminservice.pricing.tariff.domain.Tariff;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TariffRepository extends JpaRepository<Tariff, Long> {
    @EntityGraph(attributePaths = "coworking")
    List<Tariff> findAllByCoworkingIdAndArchivedFalseOrderByNameAsc(Long coworkingId);

    @EntityGraph(attributePaths = "coworking")
    Optional<Tariff> findByIdAndCoworkingIdAndArchivedFalse(Long id, Long coworkingId);

    boolean existsByCoworkingIdAndNameIgnoreCaseAndArchivedFalse(Long coworkingId, String name);
}
