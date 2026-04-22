package com.hse.adminservice.schedule.repository;

import com.hse.adminservice.schedule.entity.CoworkingScheduleException;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CoworkingScheduleExceptionRepository extends JpaRepository<CoworkingScheduleException, Long> {
    List<CoworkingScheduleException> findAllByCoworkingIdAndArchivedFalseOrderByDateAsc(Long coworkingId);

    Optional<CoworkingScheduleException> findByIdAndCoworkingIdAndArchivedFalse(Long id, Long coworkingId);

    boolean existsByCoworkingIdAndDateAndArchivedFalse(Long coworkingId, LocalDate date);
}
