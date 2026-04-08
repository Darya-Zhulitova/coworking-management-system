package com.hse.adminservice.repository;

import com.hse.adminservice.entity.CoworkingPlaceType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CoworkingPlaceTypeRepository extends JpaRepository<CoworkingPlaceType, Long> {

    @EntityGraph(attributePaths = "coworking")
    List<CoworkingPlaceType> findAllByCoworkingIdAndArchivedFalseOrderByNameAsc(Long coworkingId);

    @EntityGraph(attributePaths = "coworking")
    Optional<CoworkingPlaceType> findByIdAndCoworkingIdAndArchivedFalse(Long id, Long coworkingId);

    boolean existsByCoworkingIdAndCodeAndArchivedFalse(Long coworkingId, String code);

    boolean existsByCoworkingIdAndNameAndArchivedFalse(Long coworkingId, String name);
}
