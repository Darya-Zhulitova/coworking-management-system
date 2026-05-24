package com.hse.adminservice.calendar.application;

import com.hse.adminservice.calendar.closing.PlaceClosingCommandService;
import com.hse.adminservice.calendar.closing.dto.PlaceClosingCreateRequest;
import com.hse.adminservice.calendar.closing.dto.PlaceClosingResponse;
import com.hse.adminservice.calendar.compensation.CloseDayCommandService;
import com.hse.adminservice.calendar.compensation.dto.CloseDayRequest;
import com.hse.adminservice.calendar.exception.ScheduleExceptionCommandService;
import com.hse.adminservice.calendar.exception.dto.CoworkingScheduleExceptionCreateRequest;
import com.hse.adminservice.calendar.exception.dto.CoworkingScheduleExceptionResponse;
import com.hse.adminservice.calendar.schedule.ScheduleCommandService;
import com.hse.adminservice.calendar.schedule.dto.CoworkingScheduleDaysRequest;
import com.hse.adminservice.calendar.schedule.dto.CoworkingScheduleResponse;
import com.hse.adminservice.operations.bookingimpact.dto.OperationalImpactResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CoworkingScheduleServiceImpl implements CoworkingScheduleService {
    private final ScheduleCommandService scheduleCommandService;
    private final ScheduleExceptionCommandService scheduleExceptionCommandService;
    private final PlaceClosingCommandService placeClosingCommandService;
    private final CloseDayCommandService closeDayCommandService;

    @Override
    public CoworkingScheduleResponse getSchedule(Long coworkingId) {
        return scheduleCommandService.getSchedule(coworkingId);
    }

    @Override
    public OperationalImpactResponse previewScheduleUpdate(Long coworkingId, CoworkingScheduleDaysRequest request) {
        return scheduleCommandService.previewScheduleUpdate(coworkingId, request);
    }

    @Override
    @Transactional
    public OperationalImpactResponse commitScheduleUpdate(Long coworkingId, CoworkingScheduleDaysRequest request) {
        return scheduleCommandService.commitScheduleUpdate(coworkingId, request);
    }

    @Override
    public List<CoworkingScheduleExceptionResponse> getExceptions(Long coworkingId) {
        return scheduleExceptionCommandService.getExceptions(coworkingId);
    }

    @Override
    @Transactional
    public CoworkingScheduleExceptionResponse createException(
            Long coworkingId,
            CoworkingScheduleExceptionCreateRequest request
    ) {
        return scheduleExceptionCommandService.createException(coworkingId, request);
    }

    @Override
    public OperationalImpactResponse previewExceptionCreate(
            Long coworkingId,
            CoworkingScheduleExceptionCreateRequest request
    ) {
        return scheduleExceptionCommandService.previewExceptionCreate(coworkingId, request);
    }

    @Override
    @Transactional
    public OperationalImpactResponse commitExceptionCreate(
            Long coworkingId,
            CoworkingScheduleExceptionCreateRequest request
    ) {
        return scheduleExceptionCommandService.commitExceptionCreate(coworkingId, request);
    }

    @Override
    @Transactional
    public void archiveException(Long coworkingId, Long exceptionId) {
        scheduleExceptionCommandService.archiveException(coworkingId, exceptionId);
    }

    @Override
    public List<PlaceClosingResponse> getClosings(Long coworkingId) {
        return placeClosingCommandService.getClosings(coworkingId);
    }

    @Override
    public List<PlaceClosingResponse> getClosingsForFloor(Long coworkingId, Long floorId) {
        return placeClosingCommandService.getClosingsForFloor(coworkingId, floorId);
    }

    @Override
    public OperationalImpactResponse previewClosingCreate(Long coworkingId, PlaceClosingCreateRequest request) {
        return placeClosingCommandService.previewClosingCreate(coworkingId, request);
    }

    @Override
    @Transactional
    public OperationalImpactResponse commitClosingCreate(Long coworkingId, PlaceClosingCreateRequest request) {
        return placeClosingCommandService.commitClosingCreate(coworkingId, request);
    }

    @Override
    public OperationalImpactResponse previewCloseDay(Long coworkingId, CloseDayRequest request) {
        return closeDayCommandService.previewCloseDay(coworkingId, request);
    }

    @Override
    @Transactional
    public OperationalImpactResponse commitCloseDay(Long coworkingId, CloseDayRequest request) {
        return closeDayCommandService.commitCloseDay(coworkingId, request);
    }

    @Override
    @Transactional
    public void archiveClosing(Long coworkingId, Long closingId) {
        placeClosingCommandService.archiveClosing(coworkingId, closingId);
    }
}
