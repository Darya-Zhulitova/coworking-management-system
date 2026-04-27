package com.hse.adminservice.calendar.api;

import com.hse.adminservice.calendar.application.CoworkingScheduleService;
import com.hse.adminservice.calendar.closing.dto.PlaceClosingCreateRequest;
import com.hse.adminservice.calendar.closing.dto.PlaceClosingResponse;
import com.hse.adminservice.calendar.compensation.dto.CloseDayRequest;
import com.hse.adminservice.calendar.exception.dto.CoworkingScheduleExceptionCreateRequest;
import com.hse.adminservice.calendar.exception.dto.CoworkingScheduleExceptionResponse;
import com.hse.adminservice.calendar.schedule.dto.CoworkingScheduleDaysRequest;
import com.hse.adminservice.calendar.schedule.dto.CoworkingScheduleResponse;
import com.hse.adminservice.operations.bookingimpact.dto.OperationalImpactResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/coworkings/{coworkingId}/schedule")
@RequiredArgsConstructor
public class CoworkingScheduleController {
    private final CoworkingScheduleService scheduleService;

    @GetMapping
    public CoworkingScheduleResponse getSchedule(@PathVariable Long coworkingId) {
        return scheduleService.getSchedule(coworkingId);
    }

    @PutMapping
    public OperationalImpactResponse updateSchedule(
            @PathVariable Long coworkingId,
            @Valid @RequestBody CoworkingScheduleDaysRequest request
    ) {
        return scheduleService.commitScheduleUpdate(coworkingId, request);
    }

    @PostMapping("/deactivate/preview")
    public OperationalImpactResponse previewScheduleUpdate(
            @PathVariable Long coworkingId,
            @Valid @RequestBody CoworkingScheduleDaysRequest request
    ) {
        return scheduleService.previewScheduleUpdate(coworkingId, request);
    }

    @PostMapping("/deactivate/commit")
    public OperationalImpactResponse commitScheduleUpdate(
            @PathVariable Long coworkingId,
            @Valid @RequestBody CoworkingScheduleDaysRequest request
    ) {
        return scheduleService.commitScheduleUpdate(coworkingId, request);
    }

    @GetMapping("/exceptions")
    public List<CoworkingScheduleExceptionResponse> getExceptions(@PathVariable Long coworkingId) {
        return scheduleService.getExceptions(coworkingId);
    }

    @PostMapping("/exceptions")
    @ResponseStatus(HttpStatus.CREATED)
    public CoworkingScheduleExceptionResponse createException(
            @PathVariable Long coworkingId,
            @Valid @RequestBody
            CoworkingScheduleExceptionCreateRequest request
    ) {
        return scheduleService.createException(coworkingId, request);
    }

    @PostMapping("/exceptions/deactivate/preview")
    public OperationalImpactResponse previewExceptionCreate(
            @PathVariable Long coworkingId,
            @Valid @RequestBody
            CoworkingScheduleExceptionCreateRequest request
    ) {
        return scheduleService.previewExceptionCreate(coworkingId, request);
    }

    @PostMapping("/exceptions/deactivate/commit")
    public OperationalImpactResponse commitExceptionCreate(
            @PathVariable Long coworkingId,
            @Valid @RequestBody
            CoworkingScheduleExceptionCreateRequest request
    ) {
        return scheduleService.commitExceptionCreate(coworkingId, request);
    }

    @DeleteMapping("/exceptions/{exceptionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void archiveException(@PathVariable Long coworkingId, @PathVariable Long exceptionId) {
        scheduleService.archiveException(coworkingId, exceptionId);
    }

    @GetMapping("/closings")
    public List<PlaceClosingResponse> getClosings(
            @PathVariable Long coworkingId,
            @RequestParam(required = false) Long floorId
    ) {
        return floorId == null ? scheduleService.getClosings(coworkingId) : scheduleService.getClosingsForFloor(coworkingId,
                floorId
        );
    }

    @PostMapping("/closings")
    @ResponseStatus(HttpStatus.CREATED)
    public OperationalImpactResponse createClosing(
            @PathVariable Long coworkingId,
            @Valid @RequestBody PlaceClosingCreateRequest request
    ) {
        return scheduleService.createClosing(coworkingId, request);
    }

    @PostMapping("/closings/deactivate/preview")
    public OperationalImpactResponse previewClosing(
            @PathVariable Long coworkingId,
            @Valid @RequestBody PlaceClosingCreateRequest request
    ) {
        return scheduleService.previewClosingCreate(coworkingId, request);
    }

    @PostMapping("/closings/deactivate/commit")
    public OperationalImpactResponse commitClosing(
            @PathVariable Long coworkingId,
            @Valid @RequestBody PlaceClosingCreateRequest request
    ) {
        return scheduleService.commitClosingCreate(coworkingId, request);
    }

    @PostMapping("/close-day")
    public OperationalImpactResponse closeDay(
            @PathVariable Long coworkingId,
            @Valid @RequestBody CloseDayRequest request
    ) {
        return scheduleService.commitCloseDay(coworkingId, request);
    }

    @PostMapping("/close-day/deactivate/preview")
    public OperationalImpactResponse previewCloseDay(
            @PathVariable Long coworkingId,
            @Valid @RequestBody CloseDayRequest request
    ) {
        return scheduleService.previewCloseDay(coworkingId, request);
    }

    @PostMapping("/close-day/deactivate/commit")
    public OperationalImpactResponse commitCloseDay(
            @PathVariable Long coworkingId,
            @Valid @RequestBody CloseDayRequest request
    ) {
        return scheduleService.commitCloseDay(coworkingId, request);
    }

    @DeleteMapping("/closings/{closingId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void archiveClosing(@PathVariable Long coworkingId, @PathVariable Long closingId) {
        scheduleService.archiveClosing(coworkingId, closingId);
    }
}
