package com.hse.adminservice.space.placetype.application;

import com.hse.adminservice.common.error.ConflictException;
import com.hse.adminservice.common.error.ResourceNotFoundException;
import com.hse.adminservice.coworking.application.CoworkingConfigurationVersionService;
import com.hse.adminservice.coworking.domain.Coworking;
import com.hse.adminservice.coworking.persistence.CoworkingRepository;
import com.hse.adminservice.pricing.tariff.domain.Tariff;
import com.hse.adminservice.pricing.tariff.persistence.TariffRepository;
import com.hse.adminservice.rbac.authorization.AdminAuthorizationService;
import com.hse.adminservice.rbac.domain.Grant;
import com.hse.adminservice.space.place.persistence.PlaceRepository;
import com.hse.adminservice.space.placetype.domain.PlaceType;
import com.hse.adminservice.space.placetype.dto.PlaceTypeCreateRequest;
import com.hse.adminservice.space.placetype.dto.PlaceTypeResponse;
import com.hse.adminservice.space.placetype.dto.PlaceTypeUpdateRequest;
import com.hse.adminservice.space.placetype.mapper.PlaceTypeMapper;
import com.hse.adminservice.space.placetype.persistence.PlaceTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlaceTypeServiceImpl implements PlaceTypeService {
    private final PlaceTypeRepository placeTypeRepository;
    private final CoworkingRepository coworkingRepository;
    private final TariffRepository tariffRepository;
    private final PlaceRepository placeRepository;
    private final AdminAuthorizationService authorizationService;
    private final PlaceTypeMapper placeTypeMapper;
    private final CoworkingConfigurationVersionService configurationVersionService;

    @Override
    @Transactional
    public PlaceTypeResponse create(Long coworkingId, PlaceTypeCreateRequest request) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.PLACE_TYPE_EDIT);
        Coworking coworking = coworkingRepository.findByIdAndArchivedFalse(coworkingId)
                .orElseThrow(() -> new ResourceNotFoundException("Coworking not found"));
        String normalizedName = request.name().trim();
        if (placeTypeRepository.existsByCoworkingIdAndNameIgnoreCaseAndArchivedFalse(coworkingId, normalizedName)) {
            throw new ConflictException("Place type name must be unique within coworking");
        }
        Tariff tariff = tariffRepository.findByIdAndCoworkingIdAndArchivedFalse(request.tariffId(), coworkingId)
                .orElseThrow(() -> new ResourceNotFoundException("Tariff not found"));
        if (!Boolean.TRUE.equals(tariff.getActive())) {
            throw new ConflictException("Tariff must be active");
        }
        LocalDateTime now = LocalDateTime.now();
        PlaceType placeType = placeTypeRepository.save(PlaceType.builder().coworking(coworking).tariff(tariff).name(
                normalizedName).active(true).archived(false).createdAt(now).updatedAt(now).build());
        configurationVersionService.bumpVersion(coworkingId);
        return placeTypeMapper.toResponse(placeType);
    }

    @Override
    public List<PlaceTypeResponse> getAll(Long coworkingId) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.PLACE_TYPE_READ);
        return placeTypeRepository.findAllByCoworkingIdAndArchivedFalseOrderByNameAsc(coworkingId).stream().map(
                placeTypeMapper::toResponse).toList();
    }

    @Override
    public PlaceTypeResponse getById(Long coworkingId, Long placeTypeId) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.PLACE_TYPE_READ);
        return placeTypeMapper.toResponse(getExistingType(coworkingId, placeTypeId));
    }

    @Override
    @Transactional
    public PlaceTypeResponse update(Long coworkingId, Long placeTypeId, PlaceTypeUpdateRequest request) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.PLACE_TYPE_EDIT);
        PlaceType placeType = getExistingType(coworkingId, placeTypeId);
        String normalizedName = request.name().trim();
        if (!placeType.getName()
                .equalsIgnoreCase(normalizedName) && placeTypeRepository.existsByCoworkingIdAndNameIgnoreCaseAndArchivedFalse(coworkingId,
                normalizedName
        )) {
            throw new ConflictException("Place type name must be unique within coworking");
        }
        placeType.setName(normalizedName);
        if (request.active() != null) {
            placeType.setActive(request.active());
        }
        placeType.setUpdatedAt(LocalDateTime.now());
        PlaceType saved = placeTypeRepository.save(placeType);
        configurationVersionService.bumpVersion(coworkingId);
        return placeTypeMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void archive(Long coworkingId, Long placeTypeId) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.PLACE_TYPE_EDIT);
        PlaceType placeType = getExistingType(coworkingId, placeTypeId);
        if (placeRepository.existsByCoworkingIdAndPlaceTypeIdAndArchivedFalse(coworkingId, placeTypeId)) {
            throw new ConflictException("Cannot archive place type while non-archived places still reference it");
        }
        LocalDateTime now = LocalDateTime.now();
        placeType.setArchived(true);
        placeType.setActive(false);
        placeType.setArchivedAt(now);
        placeType.setUpdatedAt(now);
        placeTypeRepository.save(placeType);
        configurationVersionService.bumpVersion(coworkingId);
    }

    private PlaceType getExistingType(Long coworkingId, Long placeTypeId) {
        return placeTypeRepository.findByIdAndCoworkingIdAndArchivedFalse(placeTypeId, coworkingId)
                .orElseThrow(() -> new ResourceNotFoundException("Place type not found"));
    }
}
