package com.hse.adminservice.calendar.exception;

import com.hse.adminservice.calendar.compensation.OperationalImpactResponseFactory;
import com.hse.adminservice.calendar.exception.domain.CoworkingScheduleException;
import com.hse.adminservice.calendar.exception.domain.ScheduleExceptionType;
import com.hse.adminservice.calendar.exception.dto.CoworkingScheduleExceptionCreateRequest;
import com.hse.adminservice.calendar.exception.dto.CoworkingScheduleExceptionResponse;
import com.hse.adminservice.calendar.exception.persistence.CoworkingScheduleExceptionRepository;
import com.hse.adminservice.common.error.ConflictException;
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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduleExceptionCommandService {
    private final CoworkingRepository coworkingRepository;
    private final CoworkingScheduleExceptionRepository exceptionRepository;
    private final AdminAuthorizationService authorizationService;
    private final CoworkingConfigurationVersionService configurationVersionService;
    private final UserBookingImpactPort userBookingImpactPort;
    private final ScheduleExceptionValidator scheduleExceptionValidator;
    private final OperationalImpactResponseFactory impactResponseFactory;
    private final TimeProvider timeProvider;
    private final PlatformTransactionManager transactionManager;

    public List<CoworkingScheduleExceptionResponse> getExceptions(Long coworkingId) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.SCHEDULE_READ);
        return exceptionRepository.findAllByCoworkingIdAndArchivedFalseOrderByDateAsc(coworkingId)
                .stream()
                .map(this::toExceptionResponse)
                .toList();
    }

    @Transactional
    public CoworkingScheduleExceptionResponse createException(
            Long coworkingId,
            CoworkingScheduleExceptionCreateRequest request
    ) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.SCHEDULE_EDIT);
        if (request.getType() == ScheduleExceptionType.CLOSE) {
            throw new ConflictException("Сначала выполните предпросмотр изменений, затем подтвердите действие.");
        }
        Coworking coworking = getCoworking(coworkingId);
        scheduleExceptionValidator.ensureCanBeCreated(coworkingId, request.getDate());
        CoworkingScheduleException entity = saveException(coworking, request);
        configurationVersionService.bumpVersion(coworkingId);
        return toExceptionResponse(entity);
    }

    public OperationalImpactResponse previewExceptionCreate(
            Long coworkingId,
            CoworkingScheduleExceptionCreateRequest request
    ) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.SCHEDULE_EDIT);
        Coworking coworking = getCoworking(coworkingId);
        scheduleExceptionValidator.ensureCanBeCreated(coworkingId, request.getDate());
        if (request.getType() == ScheduleExceptionType.CLOSE) {
            return userBookingImpactPort.previewForCloseDay(coworking, request.getDate(), request.getName().trim());
        }
        return impactResponseFactory.noImpact(
                "SCHEDULE_EXCEPTION",
                "COWORKING_SCHEDULE_EXCEPTION",
                coworking.getId(),
                request.getName().trim(),
                request.getDate(),
                "PREVIEW"
        );
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public OperationalImpactResponse commitExceptionCreate(
            Long coworkingId,
            CoworkingScheduleExceptionCreateRequest request
    ) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.SCHEDULE_EDIT);
        Coworking coworking = getCoworking(coworkingId);
        scheduleExceptionValidator.ensureCanBeCreated(coworkingId, request.getDate());
        OperationalImpactResponse impact;
        if (request.getType() == ScheduleExceptionType.CLOSE) {
            String impactHash = requireImpactHash(request.getImpactHash());
            impact = userBookingImpactPort.commitCloseDay(
                    coworking,
                    request.getDate(),
                    request.getName().trim(),
                    impactHash
            );
        } else {
            validateNoImpactHash(request.getImpactHash());
            impact = impactResponseFactory.noImpact(
                    "SCHEDULE_EXCEPTION",
                    "COWORKING_SCHEDULE_EXCEPTION",
                    coworking.getId(),
                    request.getName().trim(),
                    request.getDate(),
                    "COMMIT"
            );
        }
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> saveCommittedException(
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

    private void validateNoImpactHash(String impactHash) {
        if (!"NO_IMPACT".equals(impactHash == null ? null : impactHash.trim())) {
            throw new ConflictException("Сначала выполните предпросмотр изменений, затем подтвердите действие.");
        }
    }

    @Transactional
    public void archiveException(Long coworkingId, Long exceptionId) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.SCHEDULE_EDIT);
        CoworkingScheduleException entity = exceptionRepository.findByIdAndCoworkingIdAndArchivedFalse(
                exceptionId,
                coworkingId
        ).orElseThrow(() -> new ResourceNotFoundException("Исключение расписания не найдено"));
        LocalDateTime now = timeProvider.now();
        entity.setActive(false);
        entity.setArchived(true);
        entity.setArchivedAt(now);
        entity.setUpdatedAt(now);
        exceptionRepository.save(entity);
        configurationVersionService.bumpVersion(coworkingId);
    }

    public CoworkingScheduleException saveException(
            Coworking coworking,
            CoworkingScheduleExceptionCreateRequest request
    ) {
        LocalDateTime now = timeProvider.now();
        return exceptionRepository.save(CoworkingScheduleException.builder()
                .coworking(coworking)
                .date(request.getDate())
                .type(request.getType())
                .name(request.getName().trim())
                .active(true)
                .archived(false)
                .createdAt(now)
                .updatedAt(now)
                .build());
    }

    private void saveCommittedException(Long coworkingId, CoworkingScheduleExceptionCreateRequest request) {
        Coworking coworking = getCoworkingForUpdate(coworkingId);
        scheduleExceptionValidator.ensureCanBeCreated(coworkingId, request.getDate());
        saveException(coworking, request);
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

    private CoworkingScheduleExceptionResponse toExceptionResponse(CoworkingScheduleException entity) {
        return CoworkingScheduleExceptionResponse.builder()
                .id(entity.getId())
                .date(entity.getDate())
                .type(entity.getType())
                .name(entity.getName())
                .active(entity.getActive())
                .build();
    }
}
