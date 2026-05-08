package com.hse.adminservice.space.placetype.persistence;

import com.hse.adminservice.space.placetype.domain.PlaceType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlaceTypeRepository extends JpaRepository<PlaceType, Long> {
    @EntityGraph(attributePaths = {"coworking", "tariff"})
    List<PlaceType> findAllByCoworkingIdAndArchivedFalseOrderByNameAsc(Long coworkingId);

    @EntityGraph(attributePaths = {"coworking", "tariff"})
    Optional<PlaceType> findByIdAndCoworkingIdAndArchivedFalse(Long id, Long coworkingId);

    boolean existsByCoworkingIdAndNameIgnoreCaseAndArchivedFalse(Long coworkingId, String name);
}
