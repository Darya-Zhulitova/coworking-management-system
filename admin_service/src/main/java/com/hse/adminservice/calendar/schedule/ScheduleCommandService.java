package com.hse.adminservice.calendar.schedule;

import com.hse.adminservice.calendar.exception.domain.ScheduleExceptionType;
import com.hse.adminservice.calendar.exception.persistence.CoworkingScheduleExceptionRepository;
import com.hse.adminservice.calendar.schedule.dto.CoworkingScheduleDaysRequest;
import com.hse.adminservice.calendar.schedule.dto.CoworkingScheduleResponse;
import com.hse.adminservice.common.error.ResourceNotFoundException;
import com.hse.adminservice.common.time.TimeProvider;
import com.hse.adminservice.coworking.application.CoworkingConfigurationVersionService;
import com.hse.adminservice.coworking.domain.Coworking;
import com.hse.adminservice.coworking.persistence.CoworkingRepository;
import com.hse.adminservice.integration.user.port.UserBookingImpactPort;
import com.hse.adminservice.operations.bookingimpact.dto.OperationalImpactResponse;
import com.hse.adminservice.rbac.authorization.AdminAuthorizationService;
import com.hse.adminservice.rbac.domain.Grant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final CoworkingConfigurationVersionService configurationVersionService;
    private final UserBookingImpactPort userBookingImpactPort;
    private final ScheduleBitmaskMapper scheduleBitmaskMapper;
    private final ScheduleReductionCalculator scheduleReductionCalculator;
    private final TimeProvider timeProvider;

    public CoworkingScheduleResponse getSchedule(Long coworkingId) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.SCHEDULE_READ);
        Coworking coworking = getCoworking(coworkingId);
        return scheduleBitmaskMapper.toResponse(coworking.getSchedule());
    }

    @Transactional
    public CoworkingScheduleResponse updateSchedule(Long coworkingId, CoworkingScheduleDaysRequest request) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.SCHEDULE_EDIT);
        Coworking coworking = getCoworking(coworkingId);
        coworking.setSchedule(scheduleBitmaskMapper.toBitmask(request));
        coworking.setUpdatedAt(timeProvider.now());
        coworkingRepository.save(coworking);
        configurationVersionService.bumpVersion(coworkingId);
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

    @Transactional
    public OperationalImpactResponse commitScheduleUpdate(Long coworkingId, CoworkingScheduleDaysRequest request) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.SCHEDULE_EDIT);
        Coworking coworking = getCoworking(coworkingId);
        int nextMask = scheduleBitmaskMapper.toBitmask(request);
        List<LocalDate> affectedDates = computeRemovedDates(
                coworkingId,
                coworking.getSchedule() == null ? 0 : coworking.getSchedule(),
                nextMask
        );
        OperationalImpactResponse impact = userBookingImpactPort.commitScheduleReduction(coworking, affectedDates);
        coworking.setSchedule(nextMask);
        coworking.setUpdatedAt(timeProvider.now());
        coworkingRepository.save(coworking);
        configurationVersionService.bumpVersion(coworkingId);
        return impact;
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

    private Coworking getCoworking(Long coworkingId) {
        return coworkingRepository.findByIdAndArchivedFalse(coworkingId)
                .orElseThrow(() -> new ResourceNotFoundException("Coworking not found"));
    }
}
