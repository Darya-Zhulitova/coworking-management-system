package com.hse.adminservice.calendar.schedule;

import com.hse.adminservice.calendar.exception.domain.ScheduleExceptionType;
import com.hse.adminservice.calendar.exception.persistence.CoworkingScheduleExceptionRepository;
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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduleCommandService {
    private static final int SCHEDULE_REDUCTION_HORIZON_DAYS = 35;

    private final CoworkingRepository coworkingRepository;
    private final CoworkingScheduleExceptionRepository exceptionRepository;
    private final AdminAuthorizationService authorizationService;
    private final UserBookingImpactPort userBookingImpactPort;
    private final ScheduleBitmaskMapper scheduleBitmaskMapper;
    private final ScheduleReductionCalculator scheduleReductionCalculator;
    private final TimeProvider timeProvider;
    private final PlatformTransactionManager transactionManager;

    public CoworkingScheduleResponse getSchedule(Long coworkingId) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.SCHEDULE_READ);
        Coworking coworking = getCoworking(coworkingId);
        return scheduleBitmaskMapper.toResponse(coworking.getSchedule());
    }

    public OperationalImpactResponse previewScheduleUpdate(Long coworkingId, CoworkingScheduleDaysRequest request) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.SCHEDULE_EDIT);
        Coworking coworking = getCoworking(coworkingId);
        List<LocalDate> affectedDates = computeRemovedDates(
                coworkingId,
                coworking.getSchedule() == null ? 0 : coworking.getSchedule(),
                scheduleBitmaskMapper.toBitmask(request)
        );
        return userBookingImpactPort.previewForScheduleReduction(coworking, affectedDates);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public OperationalImpactResponse commitScheduleUpdate(Long coworkingId, CoworkingScheduleDaysRequest request) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.SCHEDULE_EDIT);
        Coworking coworking = getCoworking(coworkingId);
        int previousMask = coworking.getSchedule() == null ? 0 : coworking.getSchedule();
        int nextMask = scheduleBitmaskMapper.toBitmask(request);
        List<LocalDate> affectedDates = computeRemovedDates(coworkingId, previousMask, nextMask);
        String impactHash = requireImpactHash(request.getImpactHash());
        OperationalImpactResponse impact = userBookingImpactPort.commitScheduleReduction(
                coworking,
                affectedDates,
                impactHash
        );
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> saveScheduleUpdate(
                coworkingId,
                previousMask,
                nextMask
        ));
        return impact;
    }

    private String requireImpactHash(String impactHash) {
        if (impactHash == null || impactHash.isBlank()) {
            throw new ConflictException("Сначала выполните предпросмотр изменений, затем подтвердите действие.");
        }
        return impactHash.trim();
    }

    private List<LocalDate> computeRemovedDates(Long coworkingId, int previousMask, int nextMask) {
        return scheduleReductionCalculator.computeRemovedDates(
                timeProvider.today(),
                SCHEDULE_REDUCTION_HORIZON_DAYS,
                previousMask,
                nextMask,
                forcedOpenDates(coworkingId)
        );
    }

    private Set<LocalDate> forcedOpenDates(Long coworkingId) {
        return exceptionRepository.findAllByCoworkingIdAndArchivedFalseOrderByDateAsc(coworkingId)
                .stream()
                .filter(item -> item.getType() == ScheduleExceptionType.OPEN && Boolean.TRUE.equals(item.getActive()))
                .map(item -> item.getDate())
                .collect(Collectors.toSet());
    }

    private void saveScheduleUpdate(Long coworkingId, int expectedPreviousMask, int nextMask) {
        Coworking coworking = getCoworkingForUpdate(coworkingId);
        int currentMask = coworking.getSchedule() == null ? 0 : coworking.getSchedule();
        if (currentMask != expectedPreviousMask) {
            throw new ConflictException("Расписание изменилось. Обновите предпросмотр перед подтверждением действия.");
        }
        coworking.setSchedule(nextMask);
        bumpVersion(coworking);
        coworkingRepository.save(coworking);
    }

    private void bumpVersion(Coworking coworking) {
        long nextVersion = (coworking.getConfigurationVersion() == null ? 0L : coworking.getConfigurationVersion()) + 1L;
        coworking.setConfigurationVersion(nextVersion);
        coworking.setUpdatedAt(timeProvider.now());
    }

    private Coworking getCoworking(Long coworkingId) {
        return coworkingRepository.findByIdAndArchivedFalse(coworkingId)
                .orElseThrow(() -> new ResourceNotFoundException("Коворкинг не найден"));
    }

    private Coworking getCoworkingForUpdate(Long coworkingId) {
        return coworkingRepository.findByIdAndArchivedFalseForUpdate(coworkingId)
                .orElseThrow(() -> new ResourceNotFoundException("Коворкинг не найден"));
    }
}
