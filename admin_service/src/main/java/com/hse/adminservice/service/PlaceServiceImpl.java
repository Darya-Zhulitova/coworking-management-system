package com.hse.adminservice.service;

import com.hse.adminservice.authorization.AdminAuthorizationService;
import com.hse.adminservice.entity.Grant;
import com.hse.adminservice.dto.PlaceCreateRequest;
import com.hse.adminservice.dto.PlaceDeactivationPreviewResponse;
import com.hse.adminservice.dto.PlaceResponse;
import com.hse.adminservice.dto.PlaceUpdateRequest;
import com.hse.adminservice.entity.Coworking;
import com.hse.adminservice.entity.CoworkingPlaceType;
import com.hse.adminservice.entity.Place;
import com.hse.adminservice.exception.ConflictException;
import com.hse.adminservice.exception.ResourceNotFoundException;
import com.hse.adminservice.mapper.PlaceMapper;
import com.hse.adminservice.repository.CoworkingPlaceTypeRepository;
import com.hse.adminservice.repository.CoworkingRepository;
import com.hse.adminservice.repository.PlaceRepository;
import com.hse.adminservice.service.internal.UserBookingImpactPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlaceServiceImpl implements PlaceService {

    private final PlaceRepository placeRepository;
    private final CoworkingRepository coworkingRepository;
    private final CoworkingPlaceTypeRepository placeTypeRepository;
    private final AdminAuthorizationService authorizationService;
    private final PlaceMapper placeMapper;
    private final CoworkingConfigurationVersionService configurationVersionService;
    private final UserBookingImpactPort userBookingImpactPort;

    @Override
    @Transactional
    public PlaceResponse create(Long coworkingId, PlaceCreateRequest request) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.PLACE_MANAGE);
        Coworking coworking = coworkingRepository.findByIdAndArchivedFalse(coworkingId)
                .orElseThrow(() -> new ResourceNotFoundException("Coworking not found"));

        if (placeRepository.existsByCoworkingIdAndNameAndArchivedFalse(coworkingId, request.getName().trim())) {
            throw new ConflictException("Place name must be unique within coworking");
        }

        CoworkingPlaceType placeType = getActiveType(coworkingId, request.getPlaceTypeId());
        LocalDateTime now = LocalDateTime.now();

        Place place = Place.builder()
                .name(request.getName().trim())
                .placeType(placeType)
                .coworking(coworking)
                .active(true)
                .archived(false)
                .archivedAt(null)
                .createdAt(now)
                .updatedAt(now)
                .build();

        Place saved = placeRepository.save(place);
        configurationVersionService.bumpVersion(coworkingId);
        return placeMapper.toResponse(saved);
    }

    @Override
    public List<PlaceResponse> getAll(Long coworkingId, Long placeTypeId) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.PLACE_VIEW);
        List<Place> places = placeTypeId == null
                ? placeRepository.findAllByCoworkingIdAndArchivedFalseOrderByNameAsc(coworkingId)
                : placeRepository.findAllByCoworkingIdAndPlaceTypeIdAndArchivedFalseOrderByNameAsc(coworkingId, placeTypeId);
        return places.stream().map(placeMapper::toResponse).toList();
    }

    @Override
    public PlaceResponse getById(Long coworkingId, Long placeId) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.PLACE_VIEW);
        return placeMapper.toResponse(getExistingPlace(coworkingId, placeId));
    }

    @Override
    @Transactional
    public PlaceResponse update(Long coworkingId, Long placeId, PlaceUpdateRequest request) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.PLACE_MANAGE);
        Place place = getExistingPlace(coworkingId, placeId);
        String normalizedName = request.getName().trim();

        if (!place.getName().equalsIgnoreCase(normalizedName)
                && placeRepository.existsByCoworkingIdAndNameAndArchivedFalse(coworkingId, normalizedName)) {
            throw new ConflictException("Place name must be unique within coworking");
        }

        place.setName(normalizedName);

        if (request.getPlaceTypeId() != null) {
            place.setPlaceType(getActiveType(coworkingId, request.getPlaceTypeId()));
        }

        if (request.getActive() != null) {
            place.setActive(request.getActive());
        }

        place.setUpdatedAt(LocalDateTime.now());
        Place saved = placeRepository.save(place);
        configurationVersionService.bumpVersion(coworkingId);
        return placeMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public PlaceDeactivationPreviewResponse deactivate(Long coworkingId, Long placeId) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.PLACE_MANAGE);
        Place place = getExistingPlace(coworkingId, placeId);
        var preview = userBookingImpactPort.previewForPlaceDeactivation(place);

        place.setActive(false);
        place.setUpdatedAt(LocalDateTime.now());
        placeRepository.save(place);
        configurationVersionService.bumpVersion(coworkingId);

        return PlaceDeactivationPreviewResponse.builder()
                .placeId(place.getId())
                .placeName(place.getName())
                .activeBeforeChange(true)
                .simulatedAffectedFutureBookings(preview.simulatedAffectedFutureBookings())
                .plannedUserDomainCommands(preview.plannedUserDomainCommands())
                .mode(preview.mode())
                .build();
    }

    @Override
    @Transactional
    public PlaceResponse activate(Long coworkingId, Long placeId) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.PLACE_MANAGE);
        Place place = getExistingPlace(coworkingId, placeId);
        if (!Boolean.TRUE.equals(place.getPlaceType().getActive())) {
            throw new ConflictException("Cannot activate place while its place type is inactive");
        }
        place.setActive(true);
        place.setUpdatedAt(LocalDateTime.now());
        Place saved = placeRepository.save(place);
        configurationVersionService.bumpVersion(coworkingId);
        return placeMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void archive(Long coworkingId, Long placeId) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.PLACE_MANAGE);
        Place place = getExistingPlace(coworkingId, placeId);

        LocalDateTime now = LocalDateTime.now();
        place.setArchived(true);
        place.setArchivedAt(now);
        place.setActive(false);
        place.setUpdatedAt(now);
        placeRepository.save(place);
        configurationVersionService.bumpVersion(coworkingId);
    }

    private Place getExistingPlace(Long coworkingId, Long placeId) {
        return placeRepository.findByIdAndCoworkingIdAndArchivedFalse(placeId, coworkingId)
                .orElseThrow(() -> new ResourceNotFoundException("Place not found"));
    }

    private CoworkingPlaceType getActiveType(Long coworkingId, Long placeTypeId) {
        CoworkingPlaceType placeType = placeTypeRepository.findByIdAndCoworkingIdAndArchivedFalse(placeTypeId, coworkingId)
                .orElseThrow(() -> new ResourceNotFoundException("Place type not found"));
        if (!Boolean.TRUE.equals(placeType.getActive())) {
            throw new ConflictException("Place type must be active");
        }
        return placeType;
    }
}
