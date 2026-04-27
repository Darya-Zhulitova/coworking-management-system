package com.hse.adminservice.space.floor.persistence;

import com.hse.adminservice.space.floor.domain.Floor;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FloorRepository extends JpaRepository<Floor, Long> {
    @EntityGraph(attributePaths = "coworking")
    List<Floor> findAllByCoworkingIdAndArchivedFalseOrderByIndexAsc(Long coworkingId);

    @EntityGraph(attributePaths = "coworking")
    Optional<Floor> findByIdAndCoworkingIdAndArchivedFalse(Long id, Long coworkingId);

    boolean existsByCoworkingIdAndIndexAndArchivedFalse(Long coworkingId, Integer index);

    boolean existsByCoworkingIdAndNameIgnoreCaseAndArchivedFalse(Long coworkingId, String name);
}
