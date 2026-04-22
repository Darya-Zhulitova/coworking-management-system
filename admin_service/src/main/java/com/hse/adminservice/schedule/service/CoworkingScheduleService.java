package com.hse.adminservice.schedule.service;

import com.hse.adminservice.coworking.dto.OperationalImpactResponse;
import com.hse.adminservice.schedule.dto.*;

import java.util.List;

public interface CoworkingScheduleService {
    CoworkingScheduleResponse getSchedule(Long coworkingId);

    CoworkingScheduleResponse updateSchedule(Long coworkingId, CoworkingScheduleDaysRequest request);

    OperationalImpactResponse previewScheduleUpdate(Long coworkingId, CoworkingScheduleDaysRequest request);

    OperationalImpactResponse commitScheduleUpdate(Long coworkingId, CoworkingScheduleDaysRequest request);

    List<CoworkingScheduleExceptionResponse> getExceptions(Long coworkingId);

    CoworkingScheduleExceptionResponse createException(
            Long coworkingId,
            CoworkingScheduleExceptionCreateRequest request
    );

    OperationalImpactResponse previewExceptionCreate(Long coworkingId, CoworkingScheduleExceptionCreateRequest request);

    OperationalImpactResponse commitExceptionCreate(Long coworkingId, CoworkingScheduleExceptionCreateRequest request);

    void archiveException(Long coworkingId, Long exceptionId);

    List<PlaceClosingResponse> getClosings(Long coworkingId);

    List<PlaceClosingResponse> getClosingsForFloor(Long coworkingId, Long floorId);

    OperationalImpactResponse createClosing(Long coworkingId, PlaceClosingCreateRequest request);

    OperationalImpactResponse previewClosingCreate(Long coworkingId, PlaceClosingCreateRequest request);

    OperationalImpactResponse commitClosingCreate(Long coworkingId, PlaceClosingCreateRequest request);

    OperationalImpactResponse closeDay(Long coworkingId, CloseDayRequest request);

    OperationalImpactResponse previewCloseDay(Long coworkingId, CloseDayRequest request);

    OperationalImpactResponse commitCloseDay(Long coworkingId, CloseDayRequest request);

    void archiveClosing(Long coworkingId, Long closingId);
}
