package com.hse.adminservice.integration.user.service;

import com.hse.adminservice.coworking.dto.OperationalImpactResponse;
import com.hse.adminservice.coworking.entity.Coworking;
import com.hse.adminservice.integration.user.dto.UserDeactivateOperationRequest;
import com.hse.adminservice.place.entity.Place;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
public class UserBookingImpactHttpPort implements UserBookingImpactPort {
    private final UserOperationDeactivationClient client;

    @Override
    public OperationalImpactResponse previewForPlaceDeactivation(Place place) {
        return client.preview(placeDeactivationRequest(place));
    }

    @Override
    public OperationalImpactResponse commitPlaceDeactivation(Place place) {
        return client.deactivate(placeDeactivationRequest(place));
    }

    @Override
    public OperationalImpactResponse previewForPlaceClosing(Place place, LocalDate date, String name) {
        return client.preview(placeClosingRequest(place, date, name));
    }

    @Override
    public OperationalImpactResponse commitPlaceClosing(Place place, LocalDate date, String name) {
        return client.deactivate(placeClosingRequest(place, date, name));
    }

    @Override
    public OperationalImpactResponse previewForCloseDay(Coworking coworking, LocalDate date, String name) {
        return client.preview(closeDayRequest(coworking, date, name));
    }

    @Override
    public OperationalImpactResponse commitCloseDay(Coworking coworking, LocalDate date, String name) {
        return client.deactivate(closeDayRequest(coworking, date, name));
    }

    @Override
    public OperationalImpactResponse previewForScheduleReduction(Coworking coworking, List<LocalDate> affectedDates) {
        return client.preview(scheduleReductionRequest(coworking, affectedDates));
    }

    @Override
    public OperationalImpactResponse commitScheduleReduction(Coworking coworking, List<LocalDate> affectedDates) {
        return client.deactivate(scheduleReductionRequest(coworking, affectedDates));
    }

    private UserDeactivateOperationRequest placeDeactivationRequest(Place place) {
        return new UserDeactivateOperationRequest(
                "PLACE_DEACTIVATION",
                "PLACE",
                place.getCoworking().getId(),
                place.getCoworking().getName(),
                place.getId(),
                place.getName(),
                place.getId(),
                place.getName(),
                List.of()
        );
    }

    private UserDeactivateOperationRequest placeClosingRequest(Place place, LocalDate date, String name) {
        return new UserDeactivateOperationRequest(
                "PLACE_CLOSING",
                "PLACE_CLOSING",
                place.getCoworking().getId(),
                place.getCoworking().getName(),
                place.getId(),
                place.getName(),
                place.getId(),
                name,
                List.of(date)
        );
    }

    private UserDeactivateOperationRequest closeDayRequest(Coworking coworking, LocalDate date, String name) {
        return new UserDeactivateOperationRequest(
                "CLOSE_DAY",
                "COWORKING_DAY",
                coworking.getId(),
                coworking.getName(),
                null,
                null,
                coworking.getId(),
                name,
                List.of(date)
        );
    }

    private UserDeactivateOperationRequest scheduleReductionRequest(
            Coworking coworking,
            List<LocalDate> affectedDates
    ) {
        return new UserDeactivateOperationRequest(
                "SCHEDULE_REDUCTION",
                "COWORKING_SCHEDULE",
                coworking.getId(),
                coworking.getName(),
                null,
                null,
                coworking.getId(),
                coworking.getName(),
                affectedDates == null ? List.of() : affectedDates
        );
    }
}
