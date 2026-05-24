package com.hse.adminservice.unit.schedule;

import com.hse.adminservice.calendar.closing.PlaceClosingValidator;
import com.hse.adminservice.calendar.closing.persistence.PlaceClosingRepository;
import com.hse.adminservice.calendar.exception.ScheduleExceptionValidator;
import com.hse.adminservice.calendar.exception.persistence.CoworkingScheduleExceptionRepository;
import com.hse.adminservice.common.error.ConflictException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScheduleValidatorsTest {
    @Mock CoworkingScheduleExceptionRepository scheduleExceptionRepository;
    @Mock PlaceClosingRepository placeClosingRepository;

    @Test
    void scheduleExceptionValidatorRejectsDuplicateActiveDateWithinCoworking() {
        ScheduleExceptionValidator validator = new ScheduleExceptionValidator(scheduleExceptionRepository);
        LocalDate date = LocalDate.of(2026, 6, 1);
        when(scheduleExceptionRepository.existsByCoworkingIdAndDateAndArchivedFalse(10L, date)).thenReturn(true);

        assertThatThrownBy(() -> validator.ensureCanBeCreated(10L, date))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Исключение расписания на эту дату уже существует.");
    }

    @Test
    void scheduleExceptionValidatorAllowsDateWithoutActiveException() {
        ScheduleExceptionValidator validator = new ScheduleExceptionValidator(scheduleExceptionRepository);
        LocalDate date = LocalDate.of(2026, 6, 1);
        when(scheduleExceptionRepository.existsByCoworkingIdAndDateAndArchivedFalse(10L, date)).thenReturn(false);

        assertThatCode(() -> validator.ensureCanBeCreated(10L, date)).doesNotThrowAnyException();
    }

    @Test
    void placeClosingValidatorRejectsDuplicateActivePlaceDate() {
        PlaceClosingValidator validator = new PlaceClosingValidator(placeClosingRepository);
        LocalDate date = LocalDate.of(2026, 6, 1);
        when(placeClosingRepository.existsByPlaceIdAndDateAndArchivedFalse(99L, date)).thenReturn(true);

        assertThatThrownBy(() -> validator.ensureCanBeCreated(99L, date))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Закрытие места на эту дату уже существует.");
    }

    @Test
    void placeClosingValidatorAllowsDateWithoutActiveClosing() {
        PlaceClosingValidator validator = new PlaceClosingValidator(placeClosingRepository);
        LocalDate date = LocalDate.of(2026, 6, 1);
        when(placeClosingRepository.existsByPlaceIdAndDateAndArchivedFalse(99L, date)).thenReturn(false);

        assertThatCode(() -> validator.ensureCanBeCreated(99L, date)).doesNotThrowAnyException();
    }
}
