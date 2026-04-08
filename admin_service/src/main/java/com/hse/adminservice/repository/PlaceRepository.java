package com.hse.adminservice.repository;

import com.hse.adminservice.entity.Place;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlaceRepository extends JpaRepository<Place, Long> {

    @EntityGraph(attributePaths = {"coworking", "placeType"})
    List<Place> findAllByCoworkingIdAndArchivedFalseOrderByNameAsc(Long coworkingId);

    @EntityGraph(attributePaths = {"coworking", "placeType"})
    List<Place> findAllByCoworkingIdAndPlaceTypeIdAndArchivedFalseOrderByNameAsc(Long coworkingId, Long placeTypeId);

    @EntityGraph(attributePaths = {"coworking", "placeType"})
    List<Place> findAllByCoworkingIdAndActiveTrueAndArchivedFalse(Long coworkingId);

    @EntityGraph(attributePaths = {"coworking", "placeType"})
    Optional<Place> findByIdAndCoworkingIdAndArchivedFalse(Long id, Long coworkingId);

    boolean existsByCoworkingIdAndNameAndArchivedFalse(Long coworkingId, String name);

    boolean existsByCoworkingIdAndPlaceTypeIdAndArchivedFalse(Long coworkingId, Long placeTypeId);
}
