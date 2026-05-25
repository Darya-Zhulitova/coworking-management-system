package com.hse.adminservice.calendar.compensation;

import com.hse.adminservice.calendar.compensation.dto.CloseDayRequest;
import com.hse.adminservice.calendar.exception.ScheduleExceptionCommandService;
import com.hse.adminservice.calendar.exception.domain.ScheduleExceptionType;
import com.hse.adminservice.calendar.exception.dto.CoworkingScheduleExceptionCreateRequest;
import com.hse.adminservice.calendar.exception.persistence.CoworkingScheduleExceptionRepository;
import com.hse.adminservice.common.error.ConflictException;
import com.hse.adminservice.common.error.ResourceNotFoundException;
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

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CloseDayCommandService {
    private final CoworkingRepository coworkingRepository;
    private final CoworkingScheduleExceptionRepository exceptionRepository;
    private final AdminAuthorizationService authorizationService;
    private final UserBookingImpactPort userBookingImpactPort;
    private final ScheduleExceptionCommandService scheduleExceptionCommandService;
    private final PlatformTransactionManager transactionManager;

    public OperationalImpactResponse previewCloseDay(Long coworkingId, CloseDayRequest request) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.SCHEDULE_EDIT);
        Coworking coworking = getCoworking(coworkingId);
        return userBookingImpactPort.previewForCloseDay(coworking, request.getDate(), request.getName().trim());
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public OperationalImpactResponse commitCloseDay(Long coworkingId, CloseDayRequest request) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.SCHEDULE_EDIT);
        Coworking coworking = getCoworking(coworkingId);
        String impactHash = requireImpactHash(request.getImpactHash());
        OperationalImpactResponse impact = userBookingImpactPort.commitCloseDay(
                coworking,
                request.getDate(),
                request.getName().trim(),
                impactHash
        );
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> saveCloseDayException(
                coworkingId,
                request
        ));
        return impact;
    }

    private String requireImpactHash(String impactHash) {
        if (impactHash == null || impactHash.isBlank()) {
            throw new ConflictException("Сначала выполните предпросмотр изменений, затем подтвердите действие.");
        }
        return impactHash.trim();
    }

    private void saveCloseDayException(Long coworkingId, CloseDayRequest request) {
        Coworking coworking = getCoworkingForUpdate(coworkingId);
        if (!exceptionRepository.existsByCoworkingIdAndDateAndArchivedFalse(coworkingId, request.getDate())) {
            CoworkingScheduleExceptionCreateRequest exceptionRequest = new CoworkingScheduleExceptionCreateRequest();
            exceptionRequest.setDate(request.getDate());
            exceptionRequest.setType(ScheduleExceptionType.CLOSE);
            exceptionRequest.setName(request.getName());
            scheduleExceptionCommandService.saveException(coworking, exceptionRequest);
            bumpVersion(coworking);
            coworkingRepository.save(coworking);
        }
    }

    private void bumpVersion(Coworking coworking) {
        long nextVersion = (coworking.getConfigurationVersion() == null ? 0L : coworking.getConfigurationVersion()) + 1L;
        coworking.setConfigurationVersion(nextVersion);
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
