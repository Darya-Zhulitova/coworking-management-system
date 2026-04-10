package com.hse.adminservice.service;

import com.hse.adminservice.authorization.AdminAuthorizationService;
import com.hse.adminservice.entity.Grant;
import com.hse.adminservice.dto.PlaceTypeCreateRequest;
import com.hse.adminservice.dto.PlaceTypeResponse;
import com.hse.adminservice.dto.PlaceTypeUpdateRequest;
import com.hse.adminservice.entity.Coworking;
import com.hse.adminservice.entity.CoworkingPlaceType;
import com.hse.adminservice.exception.ConflictException;
import com.hse.adminservice.exception.ResourceNotFoundException;
import com.hse.adminservice.mapper.PlaceTypeMapper;
import com.hse.adminservice.repository.CoworkingPlaceTypeRepository;
import com.hse.adminservice.repository.CoworkingRepository;
import com.hse.adminservice.repository.PlaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlaceTypeServiceImpl implements PlaceTypeService {

    private final CoworkingPlaceTypeRepository placeTypeRepository;
    private final CoworkingRepository coworkingRepository;
    private final PlaceRepository placeRepository;
    private final AdminAuthorizationService authorizationService;
    private final PlaceTypeMapper placeTypeMapper;
    private final CoworkingConfigurationVersionService configurationVersionService;

    @Override
    @Transactional
    public PlaceTypeResponse create(Long coworkingId, PlaceTypeCreateRequest request) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.PLACE_MANAGE);
        Coworking coworking = coworkingRepository.findByIdAndArchivedFalse(coworkingId)
                .orElseThrow(() -> new ResourceNotFoundException("Coworking not found"));

        validateUniqueness(coworkingId, request.getCode(), request.getName(), null);

        LocalDateTime now = LocalDateTime.now();
        CoworkingPlaceType placeType = CoworkingPlaceType.builder()
                .coworking(coworking)
                .code(normalizeCode(request.getCode()))
                .name(request.getName().trim())
                .description(trimToNull(request.getDescription()))
                .active(true)
                .archived(false)
                .archivedAt(null)
                .createdAt(now)
                .updatedAt(now)
                .build();

        CoworkingPlaceType saved = placeTypeRepository.save(placeType);
        configurationVersionService.bumpVersion(coworkingId);
        return placeTypeMapper.toResponse(saved);
    }

    @Override
    public List<PlaceTypeResponse> getAll(Long coworkingId) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.PLACE_VIEW);
        return placeTypeRepository.findAllByCoworkingIdAndArchivedFalseOrderByNameAsc(coworkingId).stream()
                .map(placeTypeMapper::toResponse)
                .toList();
    }

    @Override
    public PlaceTypeResponse getById(Long coworkingId, Long placeTypeId) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.PLACE_VIEW);
        CoworkingPlaceType placeType = getExistingType(coworkingId, placeTypeId);
        return placeTypeMapper.toResponse(placeType);
    }

    @Override
    @Transactional
    public PlaceTypeResponse update(Long coworkingId, Long placeTypeId, PlaceTypeUpdateRequest request) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.PLACE_MANAGE);
        CoworkingPlaceType placeType = getExistingType(coworkingId, placeTypeId);

        validateUniqueness(coworkingId, request.getCode(), request.getName(), placeTypeId);

        placeType.setCode(normalizeCode(request.getCode()));
        placeType.setName(request.getName().trim());
        placeType.setDescription(trimToNull(request.getDescription()));
        if (request.getActive() != null) {
            placeType.setActive(request.getActive());
        }
        placeType.setUpdatedAt(LocalDateTime.now());

        CoworkingPlaceType saved = placeTypeRepository.save(placeType);
        configurationVersionService.bumpVersion(coworkingId);
        return placeTypeMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void archive(Long coworkingId, Long placeTypeId) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.PLACE_MANAGE);
        CoworkingPlaceType placeType = getExistingType(coworkingId, placeTypeId);

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

    private CoworkingPlaceType getExistingType(Long coworkingId, Long placeTypeId) {
        return placeTypeRepository.findByIdAndCoworkingIdAndArchivedFalse(placeTypeId, coworkingId)
                .orElseThrow(() -> new ResourceNotFoundException("Place type not found"));
    }

    private void validateUniqueness(Long coworkingId, String rawCode, String rawName, Long currentId) {
        String code = normalizeCode(rawCode);
        String name = rawName.trim();

        boolean codeConflict = placeTypeRepository.findAllByCoworkingIdAndArchivedFalseOrderByNameAsc(coworkingId).stream()
                .anyMatch(type -> !type.getId().equals(currentId) && type.getCode().equalsIgnoreCase(code));
        if (codeConflict) {
            throw new ConflictException("Place type code must be unique within coworking");
        }

        boolean nameConflict = placeTypeRepository.findAllByCoworkingIdAndArchivedFalseOrderByNameAsc(coworkingId).stream()
                .anyMatch(type -> !type.getId().equals(currentId) && type.getName().equalsIgnoreCase(name));
        if (nameConflict) {
            throw new ConflictException("Place type name must be unique within coworking");
        }
    }

    private String normalizeCode(String value) {
        return value.trim().toUpperCase().replace(' ', '_').replace('-', '_');
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
