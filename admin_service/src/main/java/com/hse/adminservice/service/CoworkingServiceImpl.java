package com.hse.adminservice.service;

import com.hse.adminservice.authorization.AdminAuthorizationService;
import com.hse.adminservice.authorization.EffectiveAdminAction;
import com.hse.adminservice.dto.CoworkingCreateRequest;
import com.hse.adminservice.dto.CoworkingDashboardResponse;
import com.hse.adminservice.dto.CoworkingResponse;
import com.hse.adminservice.dto.CoworkingUpdateRequest;
import com.hse.adminservice.entity.Coworking;
import com.hse.adminservice.exception.ResourceNotFoundException;
import com.hse.adminservice.mapper.CoworkingMapper;
import com.hse.adminservice.repository.CoworkingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CoworkingServiceImpl implements CoworkingService {

    private final CoworkingRepository coworkingRepository;
    private final CurrentAdminService currentAdminService;
    private final AdminCoworkingAccessService accessService;
    private final AdminAuthorizationService authorizationService;
    private final AuthenticatedAdminActorService authenticatedAdminActorService;
    private final CoworkingMapper coworkingMapper;

    @Override
    @Transactional
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
        return coworkingMapper.toResponse(saved);
    }

    @Override
    public List<CoworkingResponse> getAll() {
        if (authenticatedAdminActorService.isSuperAdmin()) {
            authorizationService.requireGlobalAction(EffectiveAdminAction.VIEW_ALL_COWORKINGS);
            return coworkingRepository.findAllByArchivedFalse().stream().map(coworkingMapper::toResponse).toList();
        }

        authorizationService.requireGlobalAction(EffectiveAdminAction.VIEW_ACCESSIBLE_COWORKINGS);
        Long adminUserId = currentAdminService.getCurrentAdmin().getId();
        return accessService.getAccessibleCoworkings(adminUserId).stream()
                .map(access -> coworkingRepository.findByIdAndArchivedFalse(access.id()).orElse(null))
                .filter(java.util.Objects::nonNull)
                .map(coworkingMapper::toResponse)
                .toList();
    }

    @Override
    public CoworkingResponse getById(Long id) {
        authorizationService.requireCoworkingAction(id, EffectiveAdminAction.VIEW_COWORKING);
        Coworking coworking = coworkingRepository.findByIdAndArchivedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Coworking not found"));

        return coworkingMapper.toResponse(coworking);
    }

    @Override
    @Transactional
    public CoworkingResponse update(Long id, CoworkingUpdateRequest request) {
        authorizationService.requireCoworkingAction(id, EffectiveAdminAction.UPDATE_COWORKING);
        Coworking coworking = coworkingRepository.findByIdAndArchivedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Coworking not found"));

        coworking.setName(request.getName());
        if (request.getActive() != null) {
            coworking.setActive(request.getActive());
        }
        coworking.setUpdatedAt(LocalDateTime.now());

        return coworkingMapper.toResponse(coworkingRepository.save(coworking));
    }

    @Override
    @Transactional
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

    @Override
    public CoworkingDashboardResponse getDashboard(Long id) {
        var context = authorizationService.requireCoworkingAction(id, EffectiveAdminAction.VIEW_TENANT_DASHBOARD);
        Coworking coworking = coworkingRepository.findByIdAndArchivedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Coworking not found"));

        String subjectLabel = context.principalType().name() + "#" + (context.principalType().name().equals("SUPERADMIN")
                ? context.superAdminId()
                : context.tenantAdminUserId());

        return CoworkingDashboardResponse.builder()
                .coworking(coworkingMapper.toResponse(coworking))
                .grantedActions(context.grantedActions())
                .subjectLabel(subjectLabel)
                .build();
    }

    @Override
    public List<CoworkingResponse> getArchived() {
        authorizationService.requireGlobalAction(EffectiveAdminAction.VIEW_ALL_COWORKINGS);
        return coworkingRepository.findAllByArchivedTrue().stream().map(coworkingMapper::toResponse).toList();
    }
}
