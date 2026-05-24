package com.hse.adminservice.space.place.persistence;

import com.hse.adminservice.space.place.domain.Place;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PlaceRepository extends JpaRepository<Place, Long> {
    @EntityGraph(attributePaths = {"coworking", "floor", "placeType", "placeType.tariff"})
    List<Place> findAllByCoworkingIdAndArchivedFalseOrderByNameAsc(Long coworkingId);

    @EntityGraph(attributePaths = {"coworking", "floor", "placeType", "placeType.tariff"})
    List<Place> findAllByCoworkingIdAndFloorIdAndArchivedFalseOrderByNameAsc(Long coworkingId, Long floorId);

    @EntityGraph(attributePaths = {"coworking", "floor", "placeType", "placeType.tariff"})
    List<Place> findAllByCoworkingIdAndPlaceTypeIdAndArchivedFalseOrderByNameAsc(Long coworkingId, Long placeTypeId);

    @EntityGraph(attributePaths = {"floor", "placeType"})
    List<Place> findAllByCoworkingIdAndIdInAndArchivedFalseOrderByNameAsc(Long coworkingId, List<Long> ids);

    @EntityGraph(attributePaths = {"coworking", "floor", "placeType", "placeType.tariff"})
    Optional<Place> findByIdAndCoworkingIdAndArchivedFalse(Long id, Long coworkingId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Place p join fetch p.coworking join fetch p.floor join fetch p.placeType pt left join fetch pt.tariff where p.id = :id and p.coworking.id = :coworkingId and p.archived = false")
    Optional<Place> findByIdAndCoworkingIdAndArchivedFalseForUpdate(
            @Param("id") Long id,
            @Param("coworkingId") Long coworkingId
    );

    boolean existsByFloorIdAndNameAndArchivedFalse(Long floorId, String name);

    boolean existsByCoworkingIdAndPlaceTypeIdAndArchivedFalse(Long coworkingId, Long placeTypeId);

    boolean existsByCoworkingIdAndFloorIdAndArchivedFalse(Long coworkingId, Long floorId);
}
