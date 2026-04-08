package com.hse.adminservice.service.internal;

import com.hse.adminservice.entity.Place;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class StubUserBookingImpactPort implements UserBookingImpactPort {

    @Override
    public PlaceImpactPreview previewForPlaceDeactivation(Place place) {
        int simulatedCount = Math.toIntExact((place.getId() == null ? 0L : place.getId()) % 3L);
        return new PlaceImpactPreview(
                simulatedCount,
                List.of(
                        "would.request.user-domain.future-bookings.preview",
                        "would.enqueue.user-domain.cancellation.commands",
                        "would.enqueue.user-domain.compensation.commands"
                ),
                "stub-preview"
        );
    }
}
