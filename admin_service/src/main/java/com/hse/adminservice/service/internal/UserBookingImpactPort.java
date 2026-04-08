package com.hse.adminservice.service.internal;

import com.hse.adminservice.entity.Place;

import java.util.List;

public interface UserBookingImpactPort {
    PlaceImpactPreview previewForPlaceDeactivation(Place place);

    record PlaceImpactPreview(int simulatedAffectedFutureBookings, List<String> plannedUserDomainCommands, String mode) {}
}
