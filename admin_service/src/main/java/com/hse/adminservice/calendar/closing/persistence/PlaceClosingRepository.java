package com.hse.adminservice.calendar.closing.persistence;

import com.hse.adminservice.calendar.closing.domain.PlaceClosing;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PlaceClosingRepository extends JpaRepository<PlaceClosing, Long> {
    List<PlaceClosing> findAllByCoworkingIdAndArchivedFalseOrderByDateAsc(Long coworkingId);

    List<PlaceClosing> findAllByPlaceFloorIdAndArchivedFalseOrderByDateAsc(Long floorId);

    Optional<PlaceClosing> findByIdAndCoworkingIdAndArchivedFalse(Long id, Long coworkingId);

    boolean existsByPlaceIdAndDateAndArchivedFalse(Long placeId, LocalDate date);
}
