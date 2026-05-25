package com.hse.adminservice.space.place.application;

import com.hse.adminservice.common.error.ResourceNotFoundException;
import com.hse.adminservice.integration.user.port.UserOperationsClient;
import com.hse.adminservice.operations.booking.dto.PlaceBookingListResponse;
import com.hse.adminservice.rbac.authorization.AdminAuthorizationService;
import com.hse.adminservice.rbac.domain.Grant;
import com.hse.adminservice.space.place.domain.Place;
import com.hse.adminservice.space.place.dto.PlaceResponse;
import com.hse.adminservice.space.place.mapper.PlaceMapper;
import com.hse.adminservice.space.place.persistence.PlaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlaceQueryService {
    private final PlaceRepository placeRepository;
    private final AdminAuthorizationService authorizationService;
    private final PlaceMapper placeMapper;
    private final UserOperationsClient userOperationsClient;

    public List<PlaceResponse> getAll(Long coworkingId, Long placeTypeId) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.PLACE_READ);
        List<Place> places = placeTypeId == null ? placeRepository.findAllByCoworkingIdAndArchivedFalseOrderByNameAsc(
                coworkingId) : placeRepository.findAllByCoworkingIdAndPlaceTypeIdAndArchivedFalseOrderByNameAsc(coworkingId,
                placeTypeId
        );
        return places.stream().map(placeMapper::toResponse).toList();
    }

    public PlaceResponse getById(Long coworkingId, Long placeId) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.PLACE_READ);
        return placeMapper.toResponse(getExistingPlace(coworkingId, placeId));
    }

    public PlaceBookingListResponse getBookings(Long coworkingId, Long placeId) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.BOOKING_READ);
        Place place = getExistingPlace(coworkingId, placeId);
        return userOperationsClient.getPlaceBookings(coworkingId, place.getId());
    }

    Place getExistingPlace(Long coworkingId, Long placeId) {
        return placeRepository.findByIdAndCoworkingIdAndArchivedFalse(placeId, coworkingId)
                .orElseThrow(() -> new ResourceNotFoundException("Место не найдено"));
    }
}
