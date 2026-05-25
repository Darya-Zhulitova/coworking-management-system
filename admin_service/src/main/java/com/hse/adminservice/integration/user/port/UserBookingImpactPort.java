package com.hse.adminservice.integration.user.port;

import com.hse.adminservice.coworking.domain.Coworking;
import com.hse.adminservice.operations.bookingimpact.dto.OperationalImpactResponse;
import com.hse.adminservice.space.place.domain.Place;

import java.time.LocalDate;
import java.util.List;

public interface UserBookingImpactPort {
    OperationalImpactResponse previewForPlaceDeactivation(Place place);

    OperationalImpactResponse commitPlaceDeactivation(Place place, String impactHash);

    OperationalImpactResponse previewForPlaceClosing(Place place, LocalDate date, String name);

    OperationalImpactResponse commitPlaceClosing(Place place, LocalDate date, String name, String impactHash);

    OperationalImpactResponse previewForCloseDay(Coworking coworking, LocalDate date, String name);

    OperationalImpactResponse commitCloseDay(Coworking coworking, LocalDate date, String name, String impactHash);

    OperationalImpactResponse previewForScheduleReduction(Coworking coworking, List<LocalDate> affectedDates);

    OperationalImpactResponse commitScheduleReduction(
            Coworking coworking,
            List<LocalDate> affectedDates,
            String impactHash
    );
}
