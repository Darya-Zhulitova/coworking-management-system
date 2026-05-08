package com.hse.adminservice.space.place.application;

import com.hse.adminservice.common.error.ResourceNotFoundException;
import com.hse.adminservice.common.time.TimeProvider;
import com.hse.adminservice.coworking.application.CoworkingConfigurationVersionService;
import com.hse.adminservice.integration.user.port.UserBookingImpactPort;
import com.hse.adminservice.operations.bookingimpact.dto.OperationalImpactResponse;
import com.hse.adminservice.rbac.authorization.AdminAuthorizationService;
import com.hse.adminservice.rbac.domain.Grant;
import com.hse.adminservice.space.place.domain.Place;
import com.hse.adminservice.space.place.persistence.PlaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlaceDeactivationService {
    private final PlaceRepository placeRepository;
    private final AdminAuthorizationService authorizationService;
    private final UserBookingImpactPort userBookingImpactPort;
    private final CoworkingConfigurationVersionService configurationVersionService;
    private final TimeProvider timeProvider;

    public OperationalImpactResponse previewDeactivate(Long coworkingId, Long placeId) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.PLACE_EDIT);
        Place place = getExistingPlace(coworkingId, placeId);
        return userBookingImpactPort.previewForPlaceDeactivation(place);
    }

    @Transactional
    public OperationalImpactResponse commitDeactivate(Long coworkingId, Long placeId) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.PLACE_EDIT);
        Place place = getExistingPlace(coworkingId, placeId);
        OperationalImpactResponse result = userBookingImpactPort.commitPlaceDeactivation(place);
        place.setActive(false);
        place.setUpdatedAt(timeProvider.now());
        placeRepository.save(place);
        configurationVersionService.bumpVersion(coworkingId);
        return result;
    }

    private Place getExistingPlace(Long coworkingId, Long placeId) {
        return placeRepository.findByIdAndCoworkingIdAndArchivedFalse(placeId, coworkingId)
                .orElseThrow(() -> new ResourceNotFoundException("Place not found"));
    }
}
