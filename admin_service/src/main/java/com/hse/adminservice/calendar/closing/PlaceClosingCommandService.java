package com.hse.adminservice.calendar.closing;

import com.hse.adminservice.calendar.closing.domain.PlaceClosing;
import com.hse.adminservice.calendar.closing.dto.PlaceClosingCreateRequest;
import com.hse.adminservice.calendar.closing.dto.PlaceClosingResponse;
import com.hse.adminservice.calendar.closing.persistence.PlaceClosingRepository;
import com.hse.adminservice.common.error.ResourceNotFoundException;
import com.hse.adminservice.common.time.TimeProvider;
import com.hse.adminservice.coworking.application.CoworkingConfigurationVersionService;
import com.hse.adminservice.coworking.domain.Coworking;
import com.hse.adminservice.coworking.persistence.CoworkingRepository;
import com.hse.adminservice.integration.user.port.UserBookingImpactPort;
import com.hse.adminservice.operations.bookingimpact.dto.OperationalImpactResponse;
import com.hse.adminservice.rbac.authorization.AdminAuthorizationService;
import com.hse.adminservice.rbac.domain.Grant;
import com.hse.adminservice.space.place.domain.Place;
import com.hse.adminservice.space.place.persistence.PlaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlaceClosingCommandService {
    private final CoworkingRepository coworkingRepository;
    private final PlaceClosingRepository placeClosingRepository;
    private final PlaceRepository placeRepository;
    private final AdminAuthorizationService authorizationService;
    private final CoworkingConfigurationVersionService configurationVersionService;
    private final UserBookingImpactPort userBookingImpactPort;
    private final PlaceClosingValidator placeClosingValidator;
    private final TimeProvider timeProvider;

    public List<PlaceClosingResponse> getClosings(Long coworkingId) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.SCHEDULE_READ);
        return placeClosingRepository.findAllByCoworkingIdAndArchivedFalseOrderByDateAsc(coworkingId)
                .stream()
                .map(this::toClosingResponse)
                .toList();
    }

    public List<PlaceClosingResponse> getClosingsForFloor(Long coworkingId, Long floorId) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.SCHEDULE_READ);
        return placeClosingRepository.findAllByPlaceFloorIdAndArchivedFalseOrderByDateAsc(floorId).stream().filter(
                closing -> closing.getCoworking().getId().equals(coworkingId)).map(this::toClosingResponse).toList();
    }

    @Transactional
    public OperationalImpactResponse createClosing(Long coworkingId, PlaceClosingCreateRequest request) {
        return commitClosingCreate(coworkingId, request);
    }

    public OperationalImpactResponse previewClosingCreate(Long coworkingId, PlaceClosingCreateRequest request) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.SCHEDULE_EDIT);
        Place place = getPlaceForClosing(coworkingId, request);
        placeClosingValidator.ensureCanBeCreated(place.getId(), request.getDate());
        return userBookingImpactPort.previewForPlaceClosing(place, request.getDate(), request.getName().trim());
    }

    @Transactional
    public OperationalImpactResponse commitClosingCreate(Long coworkingId, PlaceClosingCreateRequest request) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.SCHEDULE_EDIT);
        Coworking coworking = getCoworking(coworkingId);
        Place place = getPlaceForClosing(coworkingId, request);
        placeClosingValidator.ensureCanBeCreated(place.getId(), request.getDate());
        OperationalImpactResponse impact = userBookingImpactPort.commitPlaceClosing(
                place,
                request.getDate(),
                request.getName().trim()
        );
        savePlaceClosing(coworking, place, request);
        configurationVersionService.bumpVersion(coworkingId);
        return impact;
    }

    @Transactional
    public void archiveClosing(Long coworkingId, Long closingId) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.SCHEDULE_EDIT);
        PlaceClosing entity = placeClosingRepository.findByIdAndCoworkingIdAndArchivedFalse(closingId, coworkingId)
                .orElseThrow(() -> new ResourceNotFoundException("Place closing not found"));
        LocalDateTime now = timeProvider.now();
        entity.setActive(false);
        entity.setArchived(true);
        entity.setArchivedAt(now);
        entity.setUpdatedAt(now);
        placeClosingRepository.save(entity);
        configurationVersionService.bumpVersion(coworkingId);
    }

    private Place getPlaceForClosing(Long coworkingId, PlaceClosingCreateRequest request) {
        return placeRepository.findByIdAndCoworkingIdAndArchivedFalse(request.getPlaceId(), coworkingId)
                .orElseThrow(() -> new ResourceNotFoundException("Place not found"));
    }

    private void savePlaceClosing(Coworking coworking, Place place, PlaceClosingCreateRequest request) {
        LocalDateTime now = timeProvider.now();
        placeClosingRepository.save(PlaceClosing.builder()
                .coworking(coworking)
                .place(place)
                .date(request.getDate())
                .name(request.getName().trim())
                .active(true)
                .archived(false)
                .createdAt(now)
                .updatedAt(now)
                .build());
    }

    private Coworking getCoworking(Long coworkingId) {
        return coworkingRepository.findByIdAndArchivedFalse(coworkingId)
                .orElseThrow(() -> new ResourceNotFoundException("Coworking not found"));
    }

    private PlaceClosingResponse toClosingResponse(PlaceClosing entity) {
        return PlaceClosingResponse.builder()
                .id(entity.getId())
                .placeId(entity.getPlace().getId())
                .placeName(entity.getPlace().getName())
                .floorId(entity.getPlace().getFloor().getId())
                .date(entity.getDate())
                .name(entity.getName())
                .active(entity.getActive())
                .build();
    }
}
