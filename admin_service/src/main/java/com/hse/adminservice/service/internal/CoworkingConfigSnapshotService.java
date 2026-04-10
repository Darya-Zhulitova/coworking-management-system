package com.hse.adminservice.service.internal;

import com.hse.adminservice.authorization.AdminAuthorizationService;
import com.hse.adminservice.entity.Grant;
import com.hse.adminservice.dto.internal.CoworkingConfigSnapshotResponse;
import com.hse.adminservice.entity.Coworking;
import com.hse.adminservice.exception.ResourceNotFoundException;
import com.hse.adminservice.repository.CoworkingPlaceTypeRepository;
import com.hse.adminservice.repository.CoworkingRepository;
import com.hse.adminservice.repository.PlaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CoworkingConfigSnapshotService {

    private final AdminAuthorizationService authorizationService;
    private final CoworkingRepository coworkingRepository;
    private final CoworkingPlaceTypeRepository placeTypeRepository;
    private final PlaceRepository placeRepository;

    public CoworkingConfigSnapshotResponse getSnapshot(Long coworkingId) {
        authorizationService.requireCoworkingAction(coworkingId, Grant.PLACE_VIEW);
        Coworking coworking = coworkingRepository.findByIdAndArchivedFalse(coworkingId)
                .orElseThrow(() -> new ResourceNotFoundException("Coworking not found"));

        return CoworkingConfigSnapshotResponse.builder()
                .coworkingId(coworking.getId())
                .configVersion(coworking.getConfigurationVersion())
                .generatedAt(LocalDateTime.now())
                .placeTypes(placeTypeRepository.findAllByCoworkingIdAndArchivedFalseOrderByNameAsc(coworkingId).stream()
                        .map(type -> CoworkingConfigSnapshotResponse.SnapshotPlaceTypeDto.builder()
                                .id(type.getId())
                                .code(type.getCode())
                                .name(type.getName())
                                .active(type.getActive())
                                .build())
                        .toList())
                .places(placeRepository.findAllByCoworkingIdAndArchivedFalseOrderByNameAsc(coworkingId).stream()
                        .map(place -> CoworkingConfigSnapshotResponse.SnapshotPlaceDto.builder()
                                .id(place.getId())
                                .name(place.getName())
                                .active(place.getActive())
                                .placeTypeId(place.getPlaceType().getId())
                                .build())
                        .toList())
                .build();
    }
}
