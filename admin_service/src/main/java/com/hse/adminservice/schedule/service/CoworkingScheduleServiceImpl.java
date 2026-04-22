package com.hse.adminservice.schedule.service;

import com.hse.adminservice.common.exception.ConflictException;
import com.hse.adminservice.common.exception.ResourceNotFoundException;
import com.hse.adminservice.coworking.dto.OperationalImpactResponse;
import com.hse.adminservice.coworking.entity.Coworking;
import com.hse.adminservice.coworking.repository.CoworkingRepository;
import com.hse.adminservice.coworking.service.CoworkingConfigurationVersionService;
import com.hse.adminservice.integration.user.service.UserBookingImpactPort;
import com.hse.adminservice.place.entity.Place;
import com.hse.adminservice.place.repository.PlaceRepository;
import com.hse.adminservice.rbac.authorization.AdminAuthorizationService;
import com.hse.adminservice.rbac.entity.Grant;
import com.hse.adminservice.schedule.dto.*;
import com.hse.adminservice.schedule.entity.CoworkingScheduleException;
import com.hse.adminservice.schedule.entity.PlaceClosing;
import com.hse.adminservice.schedule.entity.ScheduleExceptionType;
import com.hse.adminservice.schedule.repository.CoworkingScheduleExceptionRepository;
import com.hse.adminservice.schedule.repository.PlaceClosingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CoworkingScheduleServiceImpl implements CoworkingScheduleService {
    private static final int MONDAY_BIT = 1;
    private static final int TUESDAY_BIT = 1 << 1;
    private static final int WEDNESDAY_BIT = 1 << 2;
    private static final int THURSDAY_BIT = 1 << 3;
    private static final int FRIDAY_BIT = 1 << 4;
    private static final int SATURDAY_BIT = 1 << 5;
    private static final int SUNDAY_BIT = 1 << 6;

    private final CoworkingRepository coworkingRepository;
    private final CoworkingScheduleExceptionRepository exceptionRepository;
    private final PlaceClosingRepository placeClosingRepository;
    private final PlaceRepository placeRepository;
    private final AdminAuthorizationService authorizationService;
    private final CoworkingConfigurationVersionService configurationVersionService;
    private final UserBookingImpactPort userBookingImpactPort;

    @Override
    public CoworkingScheduleResponse getSchedule(Long coworkingId) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.SCHEDULE_READ);
        Coworking coworking = getCoworking(coworkingId);
        return toScheduleResponse(coworking.getSchedule());
    }

    @Override
    @Transactional
    public CoworkingScheduleResponse updateSchedule(Long coworkingId, CoworkingScheduleDaysRequest request) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.SCHEDULE_EDIT);
        Coworking coworking = getCoworking(coworkingId);
        coworking.setSchedule(toBitmask(request));
        coworking.setUpdatedAt(LocalDateTime.now());
        coworkingRepository.save(coworking);
        configurationVersionService.bumpVersion(coworkingId);
        return toScheduleResponse(coworking.getSchedule());
    }

    @Override
    public OperationalImpactResponse previewScheduleUpdate(Long coworkingId, CoworkingScheduleDaysRequest request) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.SCHEDULE_EDIT);
        Coworking coworking = getCoworking(coworkingId);
        List<LocalDate> affectedDates = computeRemovedDates(
                coworkingId,
                coworking.getSchedule() == null ? 0 : coworking.getSchedule(),
                toBitmask(request)
        );
        return userBookingImpactPort.previewForScheduleReduction(coworking, affectedDates);
    }

    @Override
    @Transactional
    public OperationalImpactResponse commitScheduleUpdate(Long coworkingId, CoworkingScheduleDaysRequest request) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.SCHEDULE_EDIT);
        Coworking coworking = getCoworking(coworkingId);
        List<LocalDate> affectedDates = computeRemovedDates(
                coworkingId,
                coworking.getSchedule() == null ? 0 : coworking.getSchedule(),
                toBitmask(request)
        );
        OperationalImpactResponse impact = userBookingImpactPort.commitScheduleReduction(coworking, affectedDates);
        coworking.setSchedule(toBitmask(request));
        coworking.setUpdatedAt(LocalDateTime.now());
        coworkingRepository.save(coworking);
        configurationVersionService.bumpVersion(coworkingId);
        return impact;
    }

    @Override
    public List<CoworkingScheduleExceptionResponse> getExceptions(Long coworkingId) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.SCHEDULE_READ);
        return exceptionRepository.findAllByCoworkingIdAndArchivedFalseOrderByDateAsc(coworkingId)
                .stream()
                .map(this::toExceptionResponse)
                .toList();
    }

    @Override
    @Transactional
    public CoworkingScheduleExceptionResponse createException(
            Long coworkingId,
            CoworkingScheduleExceptionCreateRequest request
    ) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.SCHEDULE_EDIT);
        Coworking coworking = getCoworking(coworkingId);
        ensureExceptionCanBeCreated(coworkingId, request);
        CoworkingScheduleException entity = saveException(coworking, request);
        configurationVersionService.bumpVersion(coworkingId);
        return toExceptionResponse(entity);
    }

    @Override
    public OperationalImpactResponse previewExceptionCreate(
            Long coworkingId,
            CoworkingScheduleExceptionCreateRequest request
    ) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.SCHEDULE_EDIT);
        Coworking coworking = getCoworking(coworkingId);
        ensureExceptionCanBeCreated(coworkingId, request);
        if (request.getType() == ScheduleExceptionType.CLOSE) {
            return userBookingImpactPort.previewForCloseDay(coworking, request.getDate(), request.getName().trim());
        }
        return noImpact(
                "SCHEDULE_EXCEPTION",
                "COWORKING_SCHEDULE_EXCEPTION",
                coworking.getId(),
                request.getName().trim(),
                request.getDate(),
                "PREVIEW"
        );
    }

    @Override
    @Transactional
    public OperationalImpactResponse commitExceptionCreate(
            Long coworkingId,
            CoworkingScheduleExceptionCreateRequest request
    ) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.SCHEDULE_EDIT);
        Coworking coworking = getCoworking(coworkingId);
        ensureExceptionCanBeCreated(coworkingId, request);
        OperationalImpactResponse impact = request.getType() == ScheduleExceptionType.CLOSE ? userBookingImpactPort.commitCloseDay(coworking,
                request.getDate(),
                request.getName().trim()
        ) : noImpact(
                "SCHEDULE_EXCEPTION",
                "COWORKING_SCHEDULE_EXCEPTION",
                coworking.getId(),
                request.getName().trim(),
                request.getDate(),
                "COMMIT"
        );
        saveException(coworking, request);
        configurationVersionService.bumpVersion(coworkingId);
        return impact;
    }

    @Override
    @Transactional
    public void archiveException(Long coworkingId, Long exceptionId) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.SCHEDULE_EDIT);
        CoworkingScheduleException entity = exceptionRepository.findByIdAndCoworkingIdAndArchivedFalse(
                        exceptionId,
                        coworkingId
                )
                .orElseThrow(() -> new ResourceNotFoundException("Schedule exception not found"));
        entity.setActive(false);
        entity.setArchived(true);
        entity.setArchivedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        exceptionRepository.save(entity);
        configurationVersionService.bumpVersion(coworkingId);
    }

    @Override
    public List<PlaceClosingResponse> getClosings(Long coworkingId) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.SCHEDULE_READ);
        return placeClosingRepository.findAllByCoworkingIdAndArchivedFalseOrderByDateAsc(coworkingId)
                .stream()
                .map(this::toClosingResponse)
                .toList();
    }

    @Override
    public List<PlaceClosingResponse> getClosingsForFloor(Long coworkingId, Long floorId) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.SCHEDULE_READ);
        return placeClosingRepository.findAllByPlaceFloorIdAndArchivedFalseOrderByDateAsc(floorId).stream().filter(
                closing -> closing.getCoworking().getId().equals(coworkingId)).map(this::toClosingResponse).toList();
    }

    @Override
    @Transactional
    public OperationalImpactResponse createClosing(Long coworkingId, PlaceClosingCreateRequest request) {
        return commitClosingCreate(coworkingId, request);
    }

    @Override
    public OperationalImpactResponse previewClosingCreate(Long coworkingId, PlaceClosingCreateRequest request) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.SCHEDULE_EDIT);
        Place place = getPlaceForClosing(coworkingId, request);
        ensureClosingCanBeCreated(place, request);
        return userBookingImpactPort.previewForPlaceClosing(place, request.getDate(), request.getName().trim());
    }

    @Override
    @Transactional
    public OperationalImpactResponse commitClosingCreate(Long coworkingId, PlaceClosingCreateRequest request) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.SCHEDULE_EDIT);
        Coworking coworking = getCoworking(coworkingId);
        Place place = getPlaceForClosing(coworkingId, request);
        ensureClosingCanBeCreated(place, request);
        OperationalImpactResponse impact = userBookingImpactPort.commitPlaceClosing(
                place,
                request.getDate(),
                request.getName().trim()
        );
        savePlaceClosing(coworking, place, request);
        configurationVersionService.bumpVersion(coworkingId);
        return impact;
    }

    @Override
    @Transactional
    public OperationalImpactResponse closeDay(Long coworkingId, CloseDayRequest request) {
        return commitCloseDay(coworkingId, request);
    }

    @Override
    public OperationalImpactResponse previewCloseDay(Long coworkingId, CloseDayRequest request) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.SCHEDULE_EDIT);
        Coworking coworking = getCoworking(coworkingId);
        return userBookingImpactPort.previewForCloseDay(coworking, request.getDate(), request.getName().trim());
    }

    @Override
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
            saveException(coworking, exceptionRequest);
            configurationVersionService.bumpVersion(coworkingId);
        }
        return impact;
    }

    @Override
    @Transactional
    public void archiveClosing(Long coworkingId, Long closingId) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.SCHEDULE_EDIT);
        PlaceClosing entity = placeClosingRepository.findByIdAndCoworkingIdAndArchivedFalse(closingId, coworkingId)
                .orElseThrow(() -> new ResourceNotFoundException("Place closing not found"));
        entity.setActive(false);
        entity.setArchived(true);
        entity.setArchivedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        placeClosingRepository.save(entity);
        configurationVersionService.bumpVersion(coworkingId);
    }

    private CoworkingScheduleException saveException(
            Coworking coworking,
            CoworkingScheduleExceptionCreateRequest request
    ) {
        LocalDateTime now = LocalDateTime.now();
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

    private void ensureExceptionCanBeCreated(Long coworkingId, CoworkingScheduleExceptionCreateRequest request) {
        if (exceptionRepository.existsByCoworkingIdAndDateAndArchivedFalse(coworkingId, request.getDate())) {
            throw new ConflictException("Schedule exception for this date already exists");
        }
    }

    private Place getPlaceForClosing(Long coworkingId, PlaceClosingCreateRequest request) {
        return placeRepository.findByIdAndCoworkingIdAndArchivedFalse(request.getPlaceId(), coworkingId)
                .orElseThrow(() -> new ResourceNotFoundException("Place not found"));
    }

    private void ensureClosingCanBeCreated(Place place, PlaceClosingCreateRequest request) {
        if (placeClosingRepository.existsByPlaceIdAndDateAndArchivedFalse(place.getId(), request.getDate())) {
            throw new ConflictException("Place closing for this date already exists");
        }
    }

    private void savePlaceClosing(Coworking coworking, Place place, PlaceClosingCreateRequest request) {
        LocalDateTime now = LocalDateTime.now();
        placeClosingRepository.save(PlaceClosing.builder()
                .coworking(coworking)
                .place(place)
                .date(request.getDate())
                .name(request.getName().trim())
                .active(true)
                .archived(false)
                .createdAt(now)
                .updatedAt(now)
                .build());
    }

    private OperationalImpactResponse noImpact(
            String operationType,
            String targetType,
            Long targetId,
            String targetName,
            LocalDate date,
            String mode
    ) {
        return OperationalImpactResponse.builder()
                .operationType(operationType)
                .targetType(targetType)
                .targetId(targetId)
                .targetName(targetName)
                .simulatedAffectedFutureBookings(0)
                .plannedUserDomainCommands(List.of())
                .affectedDates(date == null ? List.of() : List.of(date.toString()))
                .affectedBookings(List.of())
                .totalCompensationAmount(0)
                .mode(mode)
                .summary("Нет затронутых будущих бронирований.")
                .build();
    }

    private Coworking getCoworking(Long coworkingId) {
        return coworkingRepository.findByIdAndArchivedFalse(coworkingId)
                .orElseThrow(() -> new ResourceNotFoundException("Coworking not found"));
    }

    private List<LocalDate> computeRemovedDates(Long coworkingId, int previousMask, int nextMask) {
        List<LocalDate> affectedDates = new ArrayList<>();
        LocalDate start = LocalDate.now();
        LocalDate endExclusive = start.plusDays(35);
        for (LocalDate cursor = start; cursor.isBefore(endExclusive); cursor = cursor.plusDays(1)) {
            int dayBit = bitFor(cursor.getDayOfWeek());
            boolean wasOpen = (previousMask & dayBit) != 0;
            boolean nowOpen = (nextMask & dayBit) != 0;
            if (wasOpen && !nowOpen && !hasForcedOpenException(coworkingId, cursor)) {
                affectedDates.add(cursor);
            }
        }
        return affectedDates;
    }

    private boolean hasForcedOpenException(Long coworkingId, LocalDate date) {
        return exceptionRepository.findAllByCoworkingIdAndArchivedFalseOrderByDateAsc(coworkingId).stream().anyMatch(
                item -> item.getDate()
                        .equals(date) && item.getType() == ScheduleExceptionType.OPEN && Boolean.TRUE.equals(item.getActive()));
    }

    private int bitFor(DayOfWeek dayOfWeek) {
        return switch (dayOfWeek) {
            case MONDAY -> MONDAY_BIT;
            case TUESDAY -> TUESDAY_BIT;
            case WEDNESDAY -> WEDNESDAY_BIT;
            case THURSDAY -> THURSDAY_BIT;
            case FRIDAY -> FRIDAY_BIT;
            case SATURDAY -> SATURDAY_BIT;
            case SUNDAY -> SUNDAY_BIT;
        };
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

    private PlaceClosingResponse toClosingResponse(PlaceClosing entity) {
        return PlaceClosingResponse.builder()
                .id(entity.getId())
                .placeId(entity.getPlace().getId())
                .placeName(entity.getPlace().getName())
                .floorId(entity.getPlace().getFloor().getId())
                .date(entity.getDate())
                .name(entity.getName())
                .active(entity.getActive())
                .build();
    }

    private CoworkingScheduleResponse toScheduleResponse(Integer schedule) {
        int normalized = schedule == null ? 0 : schedule;
        return CoworkingScheduleResponse.builder()
                .schedule(normalized)
                .monday(hasBit(normalized, MONDAY_BIT))
                .tuesday(hasBit(normalized, TUESDAY_BIT))
                .wednesday(hasBit(normalized, WEDNESDAY_BIT))
                .thursday(hasBit(normalized, THURSDAY_BIT))
                .friday(hasBit(normalized, FRIDAY_BIT))
                .saturday(hasBit(normalized, SATURDAY_BIT))
                .sunday(hasBit(normalized, SUNDAY_BIT))
                .build();
    }

    private int toBitmask(CoworkingScheduleDaysRequest request) {
        int mask = 0;
        if (Boolean.TRUE.equals(request.getMonday()))
            mask |= MONDAY_BIT;
        if (Boolean.TRUE.equals(request.getTuesday()))
            mask |= TUESDAY_BIT;
        if (Boolean.TRUE.equals(request.getWednesday()))
            mask |= WEDNESDAY_BIT;
        if (Boolean.TRUE.equals(request.getThursday()))
            mask |= THURSDAY_BIT;
        if (Boolean.TRUE.equals(request.getFriday()))
            mask |= FRIDAY_BIT;
        if (Boolean.TRUE.equals(request.getSaturday()))
            mask |= SATURDAY_BIT;
        if (Boolean.TRUE.equals(request.getSunday()))
            mask |= SUNDAY_BIT;
        return mask;
    }

    private boolean hasBit(int schedule, int bit) {
        return (schedule & bit) != 0;
    }
}
