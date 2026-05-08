package com.hse.adminservice.calendar.compensation;

import com.hse.adminservice.calendar.compensation.dto.CloseDayRequest;
import com.hse.adminservice.calendar.exception.ScheduleExceptionCommandService;
import com.hse.adminservice.calendar.exception.domain.ScheduleExceptionType;
import com.hse.adminservice.calendar.exception.dto.CoworkingScheduleExceptionCreateRequest;
import com.hse.adminservice.calendar.exception.persistence.CoworkingScheduleExceptionRepository;
import com.hse.adminservice.common.error.ResourceNotFoundException;
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

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CloseDayCommandService {
    private final CoworkingRepository coworkingRepository;
    private final CoworkingScheduleExceptionRepository exceptionRepository;
    private final AdminAuthorizationService authorizationService;
    private final CoworkingConfigurationVersionService configurationVersionService;
    private final UserBookingImpactPort userBookingImpactPort;
    private final ScheduleExceptionCommandService scheduleExceptionCommandService;

    @Transactional
    public OperationalImpactResponse closeDay(Long coworkingId, CloseDayRequest request) {
        return commitCloseDay(coworkingId, request);
    }

    public OperationalImpactResponse previewCloseDay(Long coworkingId, CloseDayRequest request) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.SCHEDULE_EDIT);
        Coworking coworking = getCoworking(coworkingId);
        return userBookingImpactPort.previewForCloseDay(coworking, request.getDate(), request.getName().trim());
    }

    @Transactional
    public OperationalImpactResponse commitCloseDay(Long coworkingId, CloseDayRequest request) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.SCHEDULE_EDIT);
        Coworking coworking = getCoworking(coworkingId);
        OperationalImpactResponse impact = userBookingImpactPort.commitCloseDay(
                coworking,
                request.getDate(),
                request.getName().trim()
        );
        if (!exceptionRepository.existsByCoworkingIdAndDateAndArchivedFalse(coworkingId, request.getDate())) {
            CoworkingScheduleExceptionCreateRequest exceptionRequest = new CoworkingScheduleExceptionCreateRequest();
            exceptionRequest.setDate(request.getDate());
            exceptionRequest.setType(ScheduleExceptionType.CLOSE);
            exceptionRequest.setName(request.getName());
            scheduleExceptionCommandService.saveException(coworking, exceptionRequest);
            configurationVersionService.bumpVersion(coworkingId);
        }
        return impact;
    }

    private Coworking getCoworking(Long coworkingId) {
        return coworkingRepository.findByIdAndArchivedFalse(coworkingId)
                .orElseThrow(() -> new ResourceNotFoundException("Coworking not found"));
    }
}
