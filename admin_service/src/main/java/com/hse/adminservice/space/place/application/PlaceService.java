package com.hse.adminservice.space.place.application;

import com.hse.adminservice.operations.booking.dto.PlaceBookingListResponse;
import com.hse.adminservice.operations.bookingimpact.dto.OperationalImpactResponse;
import com.hse.adminservice.space.place.dto.PlaceCreateRequest;
import com.hse.adminservice.space.place.dto.PlaceResponse;
import com.hse.adminservice.space.place.dto.PlaceUpdateRequest;

import java.util.List;

public interface PlaceService {
    PlaceResponse create(Long coworkingId, PlaceCreateRequest request);

    List<PlaceResponse> getAll(Long coworkingId, Long placeTypeId);

    PlaceResponse getById(Long coworkingId, Long placeId);

    PlaceBookingListResponse getBookings(Long coworkingId, Long placeId);

    PlaceResponse update(Long coworkingId, Long placeId, PlaceUpdateRequest request);

    OperationalImpactResponse previewDeactivate(Long coworkingId, Long placeId);

    OperationalImpactResponse commitDeactivate(Long coworkingId, Long placeId);

    PlaceResponse activate(Long coworkingId, Long placeId);

    void archive(Long coworkingId, Long placeId);
}
