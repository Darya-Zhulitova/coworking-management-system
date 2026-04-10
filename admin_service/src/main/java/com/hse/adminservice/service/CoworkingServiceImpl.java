package com.hse.adminservice.service;

import com.hse.adminservice.authorization.AdminAuthorizationService;
import com.hse.adminservice.entity.Grant;
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
import java.util.LinkedHashMap;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CoworkingServiceImpl implements CoworkingService {

    private final CoworkingRepository coworkingRepository;
    private final CurrentAdminService currentAdminService;
    private final AccessService accessService;
    private final AdminAuthorizationService authorizationService;
    private final CoworkingMapper coworkingMapper;

    @Override
    @Transactional
    public CoworkingResponse create(CoworkingCreateRequest request) {
        authorizationService.requireGlobalAction(Grant.COWORKING_CREATE);
        LocalDateTime now = LocalDateTime.now();

        Coworking coworking = Coworking.builder()
                .name(request.getName().trim())
                .schedule(request.getSchedule() == null ? 127 : request.getSchedule())
                .ownerId(currentAdminService.getCurrentAdmin().getId())
                .active(true)
                .archived(false)
                .configurationVersion(0L)
                .archivedAt(null)
                .createdAt(now)
                .updatedAt(now)
                .build();

        Coworking saved = coworkingRepository.save(coworking);
        return coworkingMapper.toResponse(saved);
    }

    @Override
    public List<CoworkingResponse> getAll() {
        authorizationService.requireGlobalAction(Grant.COWORKING_LIST);
        Long adminId = currentAdminService.getCurrentAdmin().getId();
        LinkedHashMap<Long, Coworking> items = new LinkedHashMap<>();
        coworkingRepository.findAllByOwnerIdAndArchivedFalse(adminId).forEach(c -> items.put(c.getId(), c));
        accessService.getAccessibleCoworkings(adminId).forEach(access ->
                coworkingRepository.findByIdAndArchivedFalse(access.id()).ifPresent(c -> items.putIfAbsent(c.getId(), c))
        );
        return items.values().stream().map(coworkingMapper::toResponse).toList();
    }

    @Override
    public CoworkingResponse getById(Long id) {
        authorizationService.requireCoworkingAction(id, Grant.COWORKING_VIEW);
        Coworking coworking = coworkingRepository.findByIdAndArchivedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Coworking not found"));

        return coworkingMapper.toResponse(coworking);
    }

    @Override
    @Transactional
    public CoworkingResponse update(Long id, CoworkingUpdateRequest request) {
        authorizationService.requireCoworkingAction(id, Grant.COWORKING_EDIT);
        Coworking coworking = coworkingRepository.findByIdAndArchivedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Coworking not found"));

        coworking.setName(request.getName().trim());
        if (request.getSchedule() != null) {
            coworking.setSchedule(request.getSchedule());
        }
        if (request.getActive() != null) {
            coworking.setActive(request.getActive());
        }
        coworking.setUpdatedAt(LocalDateTime.now());

        return coworkingMapper.toResponse(coworkingRepository.save(coworking));
    }

    @Override
    @Transactional
    public void archive(Long id) {
        authorizationService.requireCoworkingAction(id, Grant.COWORKING_ARCHIVE);
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
        var context = authorizationService.requireCoworkingAction(id, Grant.COWORKING_DASHBOARD_VIEW);
        Coworking coworking = coworkingRepository.findByIdAndArchivedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Coworking not found"));

        String subjectLabel = "COWORKING_ADMIN#" + context.coworkingAdminId();

        return CoworkingDashboardResponse.builder()
                .coworking(coworkingMapper.toResponse(coworking))
                .grants(context.grants())
                .subjectLabel(subjectLabel)
                .build();
    }

    @Override
    public List<CoworkingResponse> getArchived() {
        authorizationService.requireGlobalAction(Grant.COWORKING_LIST);
        Long adminId = currentAdminService.getCurrentAdmin().getId();
        LinkedHashMap<Long, Coworking> items = new LinkedHashMap<>();
        coworkingRepository.findAllByArchivedTrue().stream()
                .filter(c -> adminId.equals(c.getOwnerId()))
                .forEach(c -> items.put(c.getId(), c));
        accessService.getAccessibleCoworkings(adminId).forEach(access ->
                coworkingRepository.findById(access.id())
                        .filter(Coworking::getArchived)
                        .ifPresent(c -> items.putIfAbsent(c.getId(), c))
        );
        return items.values().stream().map(coworkingMapper::toResponse).toList();
    }
}
