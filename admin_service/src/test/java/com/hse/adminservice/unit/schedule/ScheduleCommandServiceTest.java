package com.hse.adminservice.unit.schedule;

import com.hse.adminservice.calendar.exception.domain.CoworkingScheduleException;
import com.hse.adminservice.calendar.exception.domain.ScheduleExceptionType;
import com.hse.adminservice.calendar.exception.persistence.CoworkingScheduleExceptionRepository;
import com.hse.adminservice.calendar.schedule.ScheduleBitmaskMapper;
import com.hse.adminservice.calendar.schedule.ScheduleCommandService;
import com.hse.adminservice.calendar.schedule.ScheduleReductionCalculator;
import com.hse.adminservice.calendar.schedule.dto.CoworkingScheduleDaysRequest;
import com.hse.adminservice.calendar.schedule.dto.CoworkingScheduleResponse;
import com.hse.adminservice.common.error.ConflictException;
import com.hse.adminservice.common.error.ResourceNotFoundException;
import com.hse.adminservice.common.time.TimeProvider;
import com.hse.adminservice.coworking.domain.Coworking;
import com.hse.adminservice.coworking.persistence.CoworkingRepository;
import com.hse.adminservice.integration.user.port.UserBookingImpactPort;
import com.hse.adminservice.operations.bookingimpact.dto.OperationalImpactResponse;
import com.hse.adminservice.rbac.authorization.AdminAuthorizationService;
import com.hse.adminservice.rbac.domain.Grant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScheduleCommandServiceTest {
    @Mock CoworkingRepository coworkingRepository;
    @Mock CoworkingScheduleExceptionRepository exceptionRepository;
    @Mock AdminAuthorizationService authorizationService;
    @Mock UserBookingImpactPort userBookingImpactPort;
    @Mock TimeProvider timeProvider;
    @Mock PlatformTransactionManager transactionManager;

    private ScheduleCommandService service;

    @BeforeEach
    void setUp() {
        ScheduleBitmaskMapper mapper = new ScheduleBitmaskMapper();
        service = new ScheduleCommandService(
                coworkingRepository,
                exceptionRepository,
                authorizationService,
                userBookingImpactPort,
                mapper,
                new ScheduleReductionCalculator(mapper),
                timeProvider,
                transactionManager
        );
    }

    @Test
    void getScheduleRequiresReadGrantAndMapsStoredMask() {
        Coworking coworking = Coworking.builder()
                .id(10L)
                .schedule(ScheduleBitmaskMapper.MONDAY_BIT | ScheduleBitmaskMapper.FRIDAY_BIT)
                .build();
        when(coworkingRepository.findByIdAndArchivedFalse(10L)).thenReturn(Optional.of(coworking));

        CoworkingScheduleResponse response = service.getSchedule(10L);

        verify(authorizationService).requireCoworkingAction(10L, Grant.SCHEDULE_READ);
        assertThat(response.monday()).isTrue();
        assertThat(response.friday()).isTrue();
        assertThat(response.tuesday()).isFalse();
        assertThat(response.schedule()).isEqualTo(ScheduleBitmaskMapper.MONDAY_BIT | ScheduleBitmaskMapper.FRIDAY_BIT);
    }

    @Test
    void getScheduleThrowsNotFoundWhenCoworkingIsMissing() {
        when(coworkingRepository.findByIdAndArchivedFalse(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getSchedule(10L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Коворкинг не найден");
    }

    @Test
    @SuppressWarnings("unchecked")
    void previewScheduleUpdateCalculatesRemovedDatesAndIgnoresForcedOpenExceptions() {
        LocalDate today = LocalDate.of(2026, 6, 1);
        Coworking coworking = Coworking.builder()
                .id(10L)
                .name("Hub")
                .schedule(ScheduleBitmaskMapper.MONDAY_BIT | ScheduleBitmaskMapper.TUESDAY_BIT)
                .build();
        CoworkingScheduleException forcedOpenTuesday = CoworkingScheduleException.builder()
                .id(1L)
                .coworking(coworking)
                .date(LocalDate.of(2026, 6, 2))
                .type(ScheduleExceptionType.OPEN)
                .active(true)
                .archived(false)
                .createdAt(LocalDateTime.of(2026, 5, 1, 12, 0))
                .updatedAt(LocalDateTime.of(2026, 5, 1, 12, 0))
                .build();
        when(coworkingRepository.findByIdAndArchivedFalse(10L)).thenReturn(Optional.of(coworking));
        when(exceptionRepository.findAllByCoworkingIdAndArchivedFalseOrderByDateAsc(10L)).thenReturn(List.of(forcedOpenTuesday));
        when(timeProvider.today()).thenReturn(today);
        when(userBookingImpactPort.previewForScheduleReduction(org.mockito.ArgumentMatchers.eq(coworking), org.mockito.ArgumentMatchers.anyList()))
                .thenReturn(OperationalImpactResponse.builder()
                        .affectedBookingsCount(0)
                        .affectedDates(List.of())
                        .affectedBookings(List.of())
                        .totalCompensationAmount(0L)
                        .impactHash("preview")
                        .build());

        service.previewScheduleUpdate(10L, scheduleRequest(true, false, null));

        verify(authorizationService).requireCoworkingAction(10L, Grant.SCHEDULE_EDIT);
        ArgumentCaptor<List<LocalDate>> datesCaptor = ArgumentCaptor.forClass(List.class);
        verify(userBookingImpactPort).previewForScheduleReduction(org.mockito.ArgumentMatchers.eq(coworking), datesCaptor.capture());
        assertThat(datesCaptor.getValue())
                .doesNotContain(LocalDate.of(2026, 6, 2))
                .contains(LocalDate.of(2026, 6, 9));
    }

    @Test
    void commitScheduleUpdateRequiresImpactHashBeforeCallingUserDomainCommit() {
        LocalDate today = LocalDate.of(2026, 6, 1);
        Coworking coworking = Coworking.builder()
                .id(10L)
                .schedule(ScheduleBitmaskMapper.MONDAY_BIT | ScheduleBitmaskMapper.TUESDAY_BIT)
                .build();
        when(coworkingRepository.findByIdAndArchivedFalse(10L)).thenReturn(Optional.of(coworking));
        when(exceptionRepository.findAllByCoworkingIdAndArchivedFalseOrderByDateAsc(10L)).thenReturn(List.of());
        when(timeProvider.today()).thenReturn(today);

        assertThatThrownBy(() -> service.commitScheduleUpdate(10L, scheduleRequest(true, false, "  ")))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Сначала выполните предпросмотр изменений, затем подтвердите действие");

        verify(userBookingImpactPort, never()).commitScheduleReduction(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyList(), org.mockito.ArgumentMatchers.anyString());
    }

    private CoworkingScheduleDaysRequest scheduleRequest(boolean monday, boolean tuesday, String impactHash) {
        CoworkingScheduleDaysRequest request = new CoworkingScheduleDaysRequest();
        request.setMonday(monday);
        request.setTuesday(tuesday);
        request.setWednesday(false);
        request.setThursday(false);
        request.setFriday(false);
        request.setSaturday(false);
        request.setSunday(false);
        request.setImpactHash(impactHash);
        return request;
    }
}
