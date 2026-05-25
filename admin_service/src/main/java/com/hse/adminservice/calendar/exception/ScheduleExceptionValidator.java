package com.hse.adminservice.calendar.exception;

import com.hse.adminservice.calendar.exception.persistence.CoworkingScheduleExceptionRepository;
import com.hse.adminservice.common.error.ConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class ScheduleExceptionValidator {
    private final CoworkingScheduleExceptionRepository exceptionRepository;

    public void ensureCanBeCreated(Long coworkingId, LocalDate date) {
        if (exceptionRepository.existsByCoworkingIdAndDateAndArchivedFalse(coworkingId, date)) {
            throw new ConflictException("Исключение расписания на эту дату уже существует.");
        }
    }
}
