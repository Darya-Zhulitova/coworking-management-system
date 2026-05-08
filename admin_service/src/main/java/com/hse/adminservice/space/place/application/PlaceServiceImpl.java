package com.hse.adminservice.space.place.application;

import com.hse.adminservice.operations.booking.dto.PlaceBookingListResponse;
import com.hse.adminservice.operations.bookingimpact.dto.OperationalImpactResponse;
import com.hse.adminservice.space.place.dto.PlaceCreateRequest;
import com.hse.adminservice.space.place.dto.PlaceResponse;
import com.hse.adminservice.space.place.dto.PlaceUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlaceServiceImpl implements PlaceService {
    private final PlaceCommandService placeCommandService;
    private final PlaceQueryService placeQueryService;
    private final PlaceDeactivationService placeDeactivationService;

    @Override
    @Transactional
    public PlaceResponse create(Long coworkingId, PlaceCreateRequest request) {
        return placeCommandService.create(coworkingId, request);
    }

    @Override
    public List<PlaceResponse> getAll(Long coworkingId, Long placeTypeId) {
        return placeQueryService.getAll(coworkingId, placeTypeId);
    }

    @Override
    public PlaceResponse getById(Long coworkingId, Long placeId) {
        return placeQueryService.getById(coworkingId, placeId);
    }

    @Override
    public PlaceBookingListResponse getBookings(Long coworkingId, Long placeId) {
        return placeQueryService.getBookings(coworkingId, placeId);
    }

    @Override
    @Transactional
    public PlaceResponse update(Long coworkingId, Long placeId, PlaceUpdateRequest request) {
        return placeCommandService.update(coworkingId, placeId, request);
    }

    @Override
    public OperationalImpactResponse previewDeactivate(Long coworkingId, Long placeId) {
        return placeDeactivationService.previewDeactivate(coworkingId, placeId);
    }

    @Override
    @Transactional
    public OperationalImpactResponse commitDeactivate(Long coworkingId, Long placeId) {
        return placeDeactivationService.commitDeactivate(coworkingId, placeId);
    }

    @Override
    @Transactional
    public PlaceResponse activate(Long coworkingId, Long placeId) {
        return placeCommandService.activate(coworkingId, placeId);
    }

    @Override
    @Transactional
    public void archive(Long coworkingId, Long placeId) {
        placeCommandService.archive(coworkingId, placeId);
    }
}
