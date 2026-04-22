package com.hse.adminservice.place.repository;

import com.hse.adminservice.place.entity.Place;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlaceRepository extends JpaRepository<Place, Long> {
    @EntityGraph(attributePaths = {"coworking", "floor", "placeType", "placeType.tariff"})
    List<Place> findAllByCoworkingIdAndArchivedFalseOrderByNameAsc(Long coworkingId);

    @EntityGraph(attributePaths = {"coworking", "floor", "placeType", "placeType.tariff"})
    List<Place> findAllByCoworkingIdAndFloorIdAndArchivedFalseOrderByNameAsc(Long coworkingId, Long floorId);

    @EntityGraph(attributePaths = {"coworking", "floor", "placeType", "placeType.tariff"})
    List<Place> findAllByCoworkingIdAndPlaceTypeIdAndArchivedFalseOrderByNameAsc(Long coworkingId, Long placeTypeId);

    @EntityGraph(attributePaths = {"coworking", "floor", "placeType", "placeType.tariff"})
    Optional<Place> findByIdAndCoworkingIdAndArchivedFalse(Long id, Long coworkingId);

    boolean existsByFloorIdAndNameAndArchivedFalse(Long floorId, String name);

    boolean existsByCoworkingIdAndPlaceTypeIdAndArchivedFalse(Long coworkingId, Long placeTypeId);

    boolean existsByCoworkingIdAndFloorIdAndArchivedFalse(Long coworkingId, Long floorId);
}
