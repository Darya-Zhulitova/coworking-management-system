package com.hse.adminservice.service;

import com.hse.adminservice.authorization.AdminAuthorizationService;
import com.hse.adminservice.authorization.EffectiveAdminAction;
import com.hse.adminservice.dto.CoworkingCreateRequest;
import com.hse.adminservice.dto.CoworkingResponse;
import com.hse.adminservice.dto.CoworkingUpdateRequest;
import com.hse.adminservice.entity.Coworking;
import com.hse.adminservice.exception.ResourceNotFoundException;
import com.hse.adminservice.repository.CoworkingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CoworkingServiceImpl implements CoworkingService {

    private final CoworkingRepository coworkingRepository;
    private final CurrentAdminService currentAdminService;
    private final AdminCoworkingAccessService accessService;
    private final AdminAuthorizationService authorizationService;
    private final AuthenticatedAdminActorService authenticatedAdminActorService;

    @Override
    public CoworkingResponse create(CoworkingCreateRequest request) {
        authorizationService.requireGlobalAction(EffectiveAdminAction.CREATE_COWORKING);
        LocalDateTime now = LocalDateTime.now();

        Coworking coworking = Coworking.builder()
                .name(request.getName())
                .active(true)
                .archived(false)
                .archivedAt(null)
                .createdAt(now)
                .updatedAt(now)
                .build();

        Coworking saved = coworkingRepository.save(coworking);
        if (!authenticatedAdminActorService.isSuperAdmin()) {
            accessService.grantOwnerAccess(currentAdminService.getCurrentAdmin(), saved);
        }
        return mapToResponse(saved);
    }

    @Override
    public List<CoworkingResponse> getAll() {
        if (authenticatedAdminActorService.isSuperAdmin()) {
            authorizationService.requireGlobalAction(EffectiveAdminAction.VIEW_ALL_COWORKINGS);
            return coworkingRepository.findAllByArchivedFalse().stream().map(this::mapToResponse).toList();
        }

        authorizationService.requireGlobalAction(EffectiveAdminAction.VIEW_ACCESSIBLE_COWORKINGS);
        Long adminUserId = currentAdminService.getCurrentAdmin().getId();
        return accessService.getAccessibleCoworkings(adminUserId).stream().map(access -> getById(access.id())).toList();
    }

    @Override
    public CoworkingResponse getById(Long id) {
        authorizationService.requireCoworkingAction(id, EffectiveAdminAction.VIEW_COWORKING);
        Coworking coworking = coworkingRepository.findByIdAndArchivedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Coworking not found"));

        return mapToResponse(coworking);
    }

    @Override
    public CoworkingResponse update(Long id, CoworkingUpdateRequest request) {
        authorizationService.requireCoworkingAction(id, EffectiveAdminAction.UPDATE_COWORKING);
        Coworking coworking = coworkingRepository.findByIdAndArchivedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Coworking not found"));

        coworking.setName(request.getName());
        if (request.getActive() != null) {
            coworking.setActive(request.getActive());
        }
        coworking.setUpdatedAt(LocalDateTime.now());

        return mapToResponse(coworkingRepository.save(coworking));
    }

    @Override
    public void archive(Long id) {
        authorizationService.requireCoworkingAction(id, EffectiveAdminAction.ARCHIVE_COWORKING);
        Coworking coworking = coworkingRepository.findByIdAndArchivedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Coworking not found"));

        coworking.setArchived(true);
        coworking.setArchivedAt(LocalDateTime.now());
        coworking.setActive(false);
        coworking.setUpdatedAt(LocalDateTime.now());

        coworkingRepository.save(coworking);
    }

    private CoworkingResponse mapToResponse(Coworking coworking) {
        return CoworkingResponse.builder()
                .id(coworking.getId())
                .name(coworking.getName())
                .active(coworking.getActive())
                .archived(coworking.getArchived())
                .archivedAt(coworking.getArchivedAt())
                .createdAt(coworking.getCreatedAt())
                .updatedAt(coworking.getUpdatedAt())
                .build();
    }
}
