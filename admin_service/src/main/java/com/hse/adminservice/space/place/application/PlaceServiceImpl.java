package com.hse.adminservice.space.place.application;

import com.hse.adminservice.common.error.ConflictException;
import com.hse.adminservice.common.error.ResourceNotFoundException;
import com.hse.adminservice.coworking.application.CoworkingConfigurationVersionService;
import com.hse.adminservice.coworking.domain.Coworking;
import com.hse.adminservice.coworking.persistence.CoworkingRepository;
import com.hse.adminservice.integration.user.port.UserBookingImpactPort;
import com.hse.adminservice.operations.booking.dto.PlaceBookingListResponse;
import com.hse.adminservice.operations.bookingimpact.dto.OperationalImpactResponse;
import com.hse.adminservice.rbac.authorization.AdminAuthorizationService;
import com.hse.adminservice.rbac.domain.Grant;
import com.hse.adminservice.space.floor.domain.Floor;
import com.hse.adminservice.space.floor.persistence.FloorRepository;
import com.hse.adminservice.space.place.domain.Place;
import com.hse.adminservice.space.place.dto.PlaceCreateRequest;
import com.hse.adminservice.space.place.dto.PlaceResponse;
import com.hse.adminservice.space.place.dto.PlaceUpdateRequest;
import com.hse.adminservice.space.place.mapper.PlaceMapper;
import com.hse.adminservice.space.place.persistence.PlaceRepository;
import com.hse.adminservice.space.placetype.domain.PlaceType;
import com.hse.adminservice.space.placetype.persistence.PlaceTypeRepository;
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
    private final FloorRepository floorRepository;
    private final PlaceTypeRepository placeTypeRepository;
    private final AdminAuthorizationService authorizationService;
    private final PlaceMapper placeMapper;
    private final CoworkingConfigurationVersionService configurationVersionService;
    private final UserBookingImpactPort userBookingImpactPort;

    @Override
    @Transactional
    public PlaceResponse create(Long coworkingId, PlaceCreateRequest request) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.PLACE_EDIT);
        Coworking coworking = coworkingRepository.findByIdAndArchivedFalse(coworkingId)
                .orElseThrow(() -> new ResourceNotFoundException("Coworking not found"));
        Floor floor = getActiveFloor(coworkingId, request.floorId());
        PlaceType placeType = getActiveType(coworkingId, request.placeTypeId());
        if (placeRepository.existsByFloorIdAndNameAndArchivedFalse(floor.getId(), request.name().trim())) {
            throw new ConflictException("Place name must be unique within floor");
        }
        LocalDateTime now = LocalDateTime.now();
        Place place = placeRepository.save(Place.builder()
                .name(request.name().trim())
                .placeType(placeType)
                .floor(floor)
                .coworking(coworking)
                .locX(request.locX())
                .locY(request.locY())
                .amenitiesRaw(placeMapper.serializeAmenities(request.amenities()))
                .active(true)
                .archived(false)
                .createdAt(now)
                .updatedAt(now)
                .build());
        configurationVersionService.bumpVersion(coworkingId);
        return placeMapper.toResponse(place);
    }

    @Override
    public List<PlaceResponse> getAll(Long coworkingId, Long placeTypeId) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.PLACE_READ);
        List<Place> places = placeTypeId == null ? placeRepository.findAllByCoworkingIdAndArchivedFalseOrderByNameAsc(
                coworkingId) : placeRepository.findAllByCoworkingIdAndPlaceTypeIdAndArchivedFalseOrderByNameAsc(coworkingId,
                placeTypeId
        );
        return places.stream().map(placeMapper::toResponse).toList();
    }

    @Override
    public PlaceBookingListResponse getBookings(Long coworkingId, Long placeId) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.BOOKING_READ);
        Place place = getExistingPlace(coworkingId, placeId);
        return PlaceBookingListResponse.builder()
                .coworkingId(coworkingId)
                .placeId(place.getId())
                .source("stub")
                .message(null)
                .bookings(List.of())
                .build();
    }

    @Override
    public PlaceResponse getById(Long coworkingId, Long placeId) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.PLACE_READ);
        return placeMapper.toResponse(getExistingPlace(coworkingId, placeId));
    }

    @Override
    @Transactional
    public PlaceResponse update(Long coworkingId, Long placeId, PlaceUpdateRequest request) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.PLACE_EDIT);
        Place place = getExistingPlace(coworkingId, placeId);
        String normalizedName = request.name().trim();
        if (!place.getName().equalsIgnoreCase(normalizedName) && placeRepository.existsByFloorIdAndNameAndArchivedFalse(place.getFloor().getId(),
                normalizedName
        )) {
            throw new ConflictException("Place name must be unique within floor");
        }
        place.setName(normalizedName);
        place.setLocX(request.locX());
        place.setLocY(request.locY());
        place.setAmenitiesRaw(placeMapper.serializeAmenities(request.amenities()));
        if (request.active() != null)
            place.setActive(request.active());
        place.setUpdatedAt(LocalDateTime.now());
        Place saved = placeRepository.save(place);
        configurationVersionService.bumpVersion(coworkingId);
        return placeMapper.toResponse(saved);
    }

    @Override
    public OperationalImpactResponse previewDeactivate(Long coworkingId, Long placeId) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.PLACE_EDIT);
        Place place = getExistingPlace(coworkingId, placeId);
        return userBookingImpactPort.previewForPlaceDeactivation(place);
    }

    @Override
    @Transactional
    public OperationalImpactResponse commitDeactivate(Long coworkingId, Long placeId) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.PLACE_EDIT);
        Place place = getExistingPlace(coworkingId, placeId);
        OperationalImpactResponse result = userBookingImpactPort.commitPlaceDeactivation(place);

        place.setActive(false);
        place.setUpdatedAt(LocalDateTime.now());
        placeRepository.save(place);
        configurationVersionService.bumpVersion(coworkingId);

        return result;
    }

    @Override
    @Transactional
    public PlaceResponse activate(Long coworkingId, Long placeId) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.PLACE_EDIT);
        Place place = getExistingPlace(coworkingId, placeId);
        if (!Boolean.TRUE.equals(place.getPlaceType().getActive())) {
            throw new ConflictException("Cannot activate place while its place type is inactive");
        }
        if (!Boolean.TRUE.equals(place.getFloor().getActive())) {
            throw new ConflictException("Cannot activate place while its floor is inactive");
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
        authorizationService.requireCoworkingAction(coworkingId, Grant.PLACE_EDIT);
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

    private Floor getActiveFloor(Long coworkingId, Long floorId) {
        Floor floor = floorRepository.findByIdAndCoworkingIdAndArchivedFalse(floorId, coworkingId)
                .orElseThrow(() -> new ResourceNotFoundException("Floor not found"));
        if (!Boolean.TRUE.equals(floor.getActive()))
            throw new ConflictException("Floor must be active");
        return floor;
    }

    private PlaceType getActiveType(Long coworkingId, Long placeTypeId) {
        PlaceType placeType = placeTypeRepository.findByIdAndCoworkingIdAndArchivedFalse(placeTypeId, coworkingId)
                .orElseThrow(() -> new ResourceNotFoundException("Place type not found"));
        if (!Boolean.TRUE.equals(placeType.getActive()))
            throw new ConflictException("Place type must be active");
        return placeType;
    }
}
