package com.hse.adminservice.floor.service;

import com.hse.adminservice.common.exception.ConflictException;
import com.hse.adminservice.common.exception.ResourceNotFoundException;
import com.hse.adminservice.coworking.entity.Coworking;
import com.hse.adminservice.coworking.repository.CoworkingRepository;
import com.hse.adminservice.coworking.service.CoworkingConfigurationVersionService;
import com.hse.adminservice.floor.dto.FloorCreateRequest;
import com.hse.adminservice.floor.dto.FloorResponse;
import com.hse.adminservice.floor.dto.FloorUpdateRequest;
import com.hse.adminservice.floor.entity.Floor;
import com.hse.adminservice.floor.mapper.FloorMapper;
import com.hse.adminservice.floor.repository.FloorRepository;
import com.hse.adminservice.place.repository.PlaceRepository;
import com.hse.adminservice.rbac.authorization.AdminAuthorizationService;
import com.hse.adminservice.rbac.entity.Grant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FloorServiceImpl implements FloorService {
    private final FloorRepository floorRepository;
    private final CoworkingRepository coworkingRepository;
    private final PlaceRepository placeRepository;
    private final AdminAuthorizationService authorizationService;
    private final FloorMapper floorMapper;
    private final CoworkingConfigurationVersionService configurationVersionService;

    @Override
    @Transactional
    public FloorResponse create(Long coworkingId, FloorCreateRequest request) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.FLOOR_EDIT);
        Coworking coworking = coworkingRepository.findByIdAndArchivedFalse(coworkingId)
                .orElseThrow(() -> new ResourceNotFoundException("Coworking not found"));

        String normalizedName = request.name().trim();
        if (floorRepository.existsByCoworkingIdAndNameIgnoreCaseAndArchivedFalse(coworkingId, normalizedName)) {
            throw new ConflictException("Floor name must be unique within coworking");
        }

        int nextIndex = floorRepository.findAllByCoworkingIdAndArchivedFalseOrderByIndexAsc(coworkingId).stream().map(
                Floor::getIndex).max(Integer::compareTo).map(value -> value + 1).orElse(0);

        LocalDateTime now = LocalDateTime.now();
        Floor floor = floorRepository.save(Floor.builder()
                .coworking(coworking)
                .name(normalizedName)
                .index(nextIndex)
                .imageFileId(trimToNull(request.imageFileId()))
                .active(true)
                .archived(false)
                .createdAt(now)
                .updatedAt(now)
                .build());
        configurationVersionService.bumpVersion(coworkingId);
        return floorMapper.toResponse(floor);
    }

    @Override
    public List<FloorResponse> getAll(Long coworkingId) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.FLOOR_READ);
        return floorRepository.findAllByCoworkingIdAndArchivedFalseOrderByIndexAsc(coworkingId)
                .stream()
                .map(floorMapper::toResponse)
                .toList();
    }

    @Override
    public FloorResponse getById(Long coworkingId, Long floorId) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.FLOOR_READ);
        return floorMapper.toResponse(getExistingFloor(coworkingId, floorId));
    }

    @Override
    @Transactional
    public FloorResponse update(Long coworkingId, Long floorId, FloorUpdateRequest request) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.FLOOR_EDIT);
        Floor floor = getExistingFloor(coworkingId, floorId);
        String normalizedName = request.name().trim();
        if (!floor.getName()
                .equalsIgnoreCase(normalizedName) && floorRepository.existsByCoworkingIdAndNameIgnoreCaseAndArchivedFalse(coworkingId,
                normalizedName
        )) {
            throw new ConflictException("Floor name must be unique within coworking");
        }
        floor.setName(normalizedName);
        floor.setImageFileId(trimToNull(request.imageFileId()));
        if (request.active() != null)
            floor.setActive(request.active());
        floor.setUpdatedAt(LocalDateTime.now());
        Floor saved = floorRepository.save(floor);
        configurationVersionService.bumpVersion(coworkingId);
        return floorMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void archive(Long coworkingId, Long floorId) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.FLOOR_EDIT);
        if (floorRepository.findAllByCoworkingIdAndArchivedFalseOrderByIndexAsc(coworkingId).size() <= 1) {
            throw new ConflictException("Coworking must keep at least one active floor");
        }
        Floor floor = getExistingFloor(coworkingId, floorId);
        if (placeRepository.existsByCoworkingIdAndFloorIdAndArchivedFalse(coworkingId, floorId)) {
            throw new ConflictException("Cannot archive floor while non-archived places still reference it");
        }
        LocalDateTime now = LocalDateTime.now();
        floor.setArchived(true);
        floor.setActive(false);
        floor.setArchivedAt(now);
        floor.setUpdatedAt(now);
        floorRepository.save(floor);
        configurationVersionService.bumpVersion(coworkingId);
    }

    private Floor getExistingFloor(Long coworkingId, Long floorId) {
        return floorRepository.findByIdAndCoworkingIdAndArchivedFalse(floorId, coworkingId)
                .orElseThrow(() -> new ResourceNotFoundException("Floor not found"));
    }

    private String trimToNull(String value) {
        if (value == null)
            return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
