package com.hse.adminservice.calendar.closing;

import com.hse.adminservice.calendar.closing.persistence.PlaceClosingRepository;
import com.hse.adminservice.common.error.ConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class PlaceClosingValidator {
    private final PlaceClosingRepository placeClosingRepository;

    public void ensureCanBeCreated(Long placeId, LocalDate date) {
        if (placeClosingRepository.existsByPlaceIdAndDateAndArchivedFalse(placeId, date)) {
            throw new ConflictException("Закрытие места на эту дату уже существует.");
        }
    }
}
