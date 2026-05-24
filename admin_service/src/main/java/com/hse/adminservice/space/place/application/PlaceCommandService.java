package com.hse.adminservice.space.place.application;

import com.hse.adminservice.common.error.ConflictException;
import com.hse.adminservice.common.error.ResourceNotFoundException;
import com.hse.adminservice.common.time.TimeProvider;
import com.hse.adminservice.coworking.application.CoworkingConfigurationVersionService;
import com.hse.adminservice.coworking.domain.Coworking;
import com.hse.adminservice.coworking.persistence.CoworkingRepository;
import com.hse.adminservice.images.ImageFileKeys;
import com.hse.adminservice.images.ImageStorageService;
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
import com.hse.adminservice.space.place.validation.PlaceActivationValidator;
import com.hse.adminservice.space.placetype.domain.PlaceType;
import com.hse.adminservice.space.placetype.persistence.PlaceTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlaceCommandService {
    private final PlaceRepository placeRepository;
    private final CoworkingRepository coworkingRepository;
    private final FloorRepository floorRepository;
    private final PlaceTypeRepository placeTypeRepository;
    private final AdminAuthorizationService authorizationService;
    private final PlaceMapper placeMapper;
    private final CoworkingConfigurationVersionService configurationVersionService;
    private final PlaceActivationValidator placeActivationValidator;
    private final TimeProvider timeProvider;
    private final ImageStorageService imageStorageService;

    @Transactional
    public PlaceResponse create(Long coworkingId, PlaceCreateRequest request) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.PLACE_EDIT);
        Coworking coworking = coworkingRepository.findByIdAndArchivedFalse(coworkingId)
                .orElseThrow(() -> new ResourceNotFoundException("Коворкинг не найден"));
        Floor floor = getActiveFloor(coworkingId, request.floorId());
        PlaceType placeType = getActiveType(coworkingId, request.placeTypeId());
        if (placeRepository.existsByFloorIdAndNameAndArchivedFalse(floor.getId(), request.name().trim())) {
            throw new ConflictException("Название места должно быть уникальным в рамках этажа");
        }
        validateCoordinates(request.locX(), request.locY());
        LocalDateTime now = timeProvider.now();
        Place place = placeRepository.save(Place.builder()
                .name(request.name().trim())
                .placeType(placeType)
                .floor(floor)
                .coworking(coworking)
                .locX(request.locX())
                .locY(request.locY())
                .imageFileId(normalizeImageFileId(request.imageFileId()))
                .fullImageFileId(normalizeImageFileId(request.imageFileId()))
                .amenitiesRaw(placeMapper.serializeAmenities(request.amenities()))
                .active(true)
                .archived(false)
                .createdAt(now)
                .updatedAt(now)
                .build());
        configurationVersionService.bumpVersion(coworkingId);
        return placeMapper.toResponse(place);
    }

    @Transactional
    public PlaceResponse update(Long coworkingId, Long placeId, PlaceUpdateRequest request) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.PLACE_EDIT);
        Place place = getExistingPlace(coworkingId, placeId);
        String normalizedName = request.name().trim();
        if (!place.getName().equalsIgnoreCase(normalizedName) && placeRepository.existsByFloorIdAndNameAndArchivedFalse(place.getFloor().getId(),
                normalizedName
        )) {
            throw new ConflictException("Название места должно быть уникальным в рамках этажа");
        }
        validateCoordinates(request.locX(), request.locY());
        place.setName(normalizedName);
        place.setLocX(request.locX());
        place.setLocY(request.locY());
        place.setAmenitiesRaw(placeMapper.serializeAmenities(request.amenities()));
        if (request.active() != null) {
            place.setActive(request.active());
        }
        place.setUpdatedAt(timeProvider.now());
        Place saved = placeRepository.save(place);
        configurationVersionService.bumpVersion(coworkingId);
        return placeMapper.toResponse(saved);
    }

    @Transactional
    public PlaceResponse uploadPhoto(Long coworkingId, Long placeId, MultipartFile file) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.PLACE_EDIT);
        Place place = getExistingPlace(coworkingId, placeId);
        ImageFileKeys image = imageStorageService.uploadPlacePhoto(placeId, file);
        place.setImageFileId(image.fullKey());
        place.setPreviewImageFileId(image.previewKey());
        place.setFullImageFileId(image.fullKey());
        place.setUpdatedAt(timeProvider.now());
        Place saved = placeRepository.save(place);
        configurationVersionService.bumpVersion(coworkingId);
        return placeMapper.toResponse(saved);
    }

    @Transactional
    public PlaceResponse activate(Long coworkingId, Long placeId) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.PLACE_EDIT);
        Place place = getExistingPlace(coworkingId, placeId);
        placeActivationValidator.validateCanActivate(place);
        place.setActive(true);
        place.setUpdatedAt(timeProvider.now());
        Place saved = placeRepository.save(place);
        configurationVersionService.bumpVersion(coworkingId);
        return placeMapper.toResponse(saved);
    }

    @Transactional
    public void archive(Long coworkingId, Long placeId) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.PLACE_EDIT);
        Place place = getExistingPlace(coworkingId, placeId);
        LocalDateTime now = timeProvider.now();
        place.setArchived(true);
        place.setArchivedAt(now);
        place.setActive(false);
        place.setUpdatedAt(now);
        placeRepository.save(place);
        configurationVersionService.bumpVersion(coworkingId);
    }

    private String normalizeImageFileId(String imageFileId) {
        return imageFileId == null || imageFileId.isBlank() ? null : imageFileId.trim();
    }

    private void validateCoordinates(Object locX, Object locY) {
        if ((locX == null) != (locY == null)) {
            throw new ConflictException("Координаты X и Y должны быть заполнены вместе или оставлены пустыми вместе");
        }
    }

    private Place getExistingPlace(Long coworkingId, Long placeId) {
        return placeRepository.findByIdAndCoworkingIdAndArchivedFalse(placeId, coworkingId)
                .orElseThrow(() -> new ResourceNotFoundException("Место не найдено"));
    }

    private Floor getActiveFloor(Long coworkingId, Long floorId) {
        Floor floor = floorRepository.findByIdAndCoworkingIdAndArchivedFalse(floorId, coworkingId)
                .orElseThrow(() -> new ResourceNotFoundException("Этаж не найден"));
        if (!Boolean.TRUE.equals(floor.getActive())) {
            throw new ConflictException("Этаж должен быть активным");
        }
        return floor;
    }

    private PlaceType getActiveType(Long coworkingId, Long placeTypeId) {
        PlaceType placeType = placeTypeRepository.findByIdAndCoworkingIdAndArchivedFalse(placeTypeId, coworkingId)
                .orElseThrow(() -> new ResourceNotFoundException("Тип места не найден"));
        if (!Boolean.TRUE.equals(placeType.getActive())) {
            throw new ConflictException("Тип места должен быть активным");
        }
        return placeType;
    }
}
