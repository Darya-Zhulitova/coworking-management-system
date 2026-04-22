package com.hse.adminservice.integration.user.service;

import com.hse.adminservice.coworking.dto.OperationalImpactResponse;
import com.hse.adminservice.coworking.entity.Coworking;
import com.hse.adminservice.place.entity.Place;

import java.time.LocalDate;
import java.util.List;

public interface UserBookingImpactPort {
    OperationalImpactResponse previewForPlaceDeactivation(Place place);

    OperationalImpactResponse commitPlaceDeactivation(Place place);

    OperationalImpactResponse previewForPlaceClosing(Place place, LocalDate date, String name);

    OperationalImpactResponse commitPlaceClosing(Place place, LocalDate date, String name);

    OperationalImpactResponse previewForCloseDay(Coworking coworking, LocalDate date, String name);

    OperationalImpactResponse commitCloseDay(Coworking coworking, LocalDate date, String name);

    OperationalImpactResponse previewForScheduleReduction(Coworking coworking, List<LocalDate> affectedDates);

    OperationalImpactResponse commitScheduleReduction(Coworking coworking, List<LocalDate> affectedDates);
}
