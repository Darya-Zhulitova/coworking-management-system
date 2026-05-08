package com.hse.adminservice.space.floor.application;

import com.hse.adminservice.common.error.ConflictException;
import com.hse.adminservice.common.error.ResourceNotFoundException;
import com.hse.adminservice.common.time.TimeProvider;
import com.hse.adminservice.coworking.application.CoworkingConfigurationVersionService;
import com.hse.adminservice.coworking.domain.Coworking;
import com.hse.adminservice.coworking.persistence.CoworkingRepository;
import com.hse.adminservice.files.FileStorageService;
import com.hse.adminservice.rbac.authorization.AdminAuthorizationService;
import com.hse.adminservice.rbac.domain.Grant;
import com.hse.adminservice.space.floor.domain.Floor;
import com.hse.adminservice.space.floor.dto.FloorCreateRequest;
import com.hse.adminservice.space.floor.dto.FloorResponse;
import com.hse.adminservice.space.floor.dto.FloorUpdateRequest;
import com.hse.adminservice.space.floor.mapper.FloorMapper;
import com.hse.adminservice.space.floor.persistence.FloorRepository;
import com.hse.adminservice.space.place.persistence.PlaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

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
    private final TimeProvider timeProvider;
    private final FileStorageService fileStorageService;

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

        LocalDateTime now = timeProvider.now();
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
        floor.setUpdatedAt(timeProvider.now());
        Floor saved = floorRepository.save(floor);
        configurationVersionService.bumpVersion(coworkingId);
        return floorMapper.toResponse(saved);
    }


    @Override
    @Transactional
    public FloorResponse uploadPlan(Long coworkingId, Long floorId, MultipartFile file) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.FLOOR_EDIT);
        Floor floor = getExistingFloor(coworkingId, floorId);
        String fileId = fileStorageService.uploadFloorPlan(floorId, file).fileId();
        floor.setImageFileId(fileId);
        floor.setUpdatedAt(timeProvider.now());
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
        LocalDateTime now = timeProvider.now();
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
