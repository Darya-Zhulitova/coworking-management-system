package com.hse.adminservice.coworking.application;

import com.hse.adminservice.admincontext.application.CurrentAdminService;
import com.hse.adminservice.common.error.ResourceNotFoundException;
import com.hse.adminservice.common.time.TimeProvider;
import com.hse.adminservice.coworking.domain.Coworking;
import com.hse.adminservice.coworking.dto.CoworkingCreateRequest;
import com.hse.adminservice.coworking.dto.CoworkingDashboardResponse;
import com.hse.adminservice.coworking.dto.CoworkingResponse;
import com.hse.adminservice.coworking.dto.CoworkingUpdateRequest;
import com.hse.adminservice.coworking.mapper.CoworkingMapper;
import com.hse.adminservice.coworking.persistence.CoworkingRepository;
import com.hse.adminservice.rbac.application.AccessService;
import com.hse.adminservice.rbac.authorization.AdminAuthorizationService;
import com.hse.adminservice.rbac.domain.Grant;
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
    private final TimeProvider timeProvider;

    @Override
    @Transactional
    public CoworkingResponse create(CoworkingCreateRequest request) {
        authorizationService.requireGlobalAction(Grant.COWORKING_EDIT);
        LocalDateTime now = timeProvider.now();

        Coworking coworking = Coworking.builder()
                .name(request.name().trim())
                .description(request.description().trim())
                .address(request.address().trim())
                .workingHoursLabel(request.workingHoursLabel().trim())
                .heroTitle(trimToNull(request.heroTitle()))
                .heroText(trimToNull(request.heroText()))
                .imageUrlsJson(coworkingMapper.writeImageUrls(request.imageUrls()))
                .schedule(127)
                .ownerId(currentAdminService.getCurrentAdmin().getId())
                .autoApproveMembership(Boolean.TRUE.equals(request.autoApproveMembership()))
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
        authorizationService.requireGlobalAction(Grant.COWORKING_READ);
        Long adminId = currentAdminService.getCurrentAdmin().getId();
        LinkedHashMap<Long, Coworking> items = new LinkedHashMap<>();
        coworkingRepository.findAllByOwnerIdAndArchivedFalse(adminId).forEach(c -> items.put(c.getId(), c));
        accessService.getAccessibleCoworkings(adminId).forEach(access -> coworkingRepository.findByIdAndArchivedFalse(
                access.id()).ifPresent(c -> items.putIfAbsent(c.getId(), c)));
        return items.values().stream().map(coworkingMapper::toResponse).toList();
    }

    @Override
    public CoworkingResponse getById(Long id) {
        authorizationService.requireCoworkingAction(id, Grant.COWORKING_READ);
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

        coworking.setName(request.name().trim());
        coworking.setDescription(request.description().trim());
        coworking.setAddress(request.address().trim());
        coworking.setWorkingHoursLabel(request.workingHoursLabel().trim());
        coworking.setHeroTitle(trimToNull(request.heroTitle()));
        coworking.setHeroText(trimToNull(request.heroText()));
        coworking.setImageUrlsJson(coworkingMapper.writeImageUrls(request.imageUrls()));
        if (request.active() != null) {
            coworking.setActive(request.active());
        }
        if (request.autoApproveMembership() != null) {
            coworking.setAutoApproveMembership(request.autoApproveMembership());
        }
        coworking.setUpdatedAt(timeProvider.now());

        return coworkingMapper.toResponse(coworkingRepository.save(coworking));
    }

    @Override
    @Transactional
    public void archive(Long id) {
        authorizationService.requireCoworkingAction(id, Grant.COWORKING_EDIT);
        Coworking coworking = coworkingRepository.findByIdAndArchivedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Coworking not found"));

        coworking.setArchived(true);
        coworking.setArchivedAt(timeProvider.now());
        coworking.setActive(false);
        coworking.setUpdatedAt(timeProvider.now());

        coworkingRepository.save(coworking);
    }

    @Override
    public CoworkingDashboardResponse getDashboard(Long id) {
        var context = authorizationService.requireCoworkingAction(id, Grant.COWORKING_READ);
        Coworking coworking = coworkingRepository.findByIdAndArchivedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Coworking not found"));

        return CoworkingDashboardResponse.builder()
                .coworking(coworkingMapper.toResponse(coworking))
                .grants(context.grants())
                .subjectLabel("ADMIN#" + context.adminId())
                .build();
    }

    @Override
    public List<CoworkingResponse> getArchived() {
        authorizationService.requireGlobalAction(Grant.COWORKING_READ);
        Long adminId = currentAdminService.getCurrentAdmin().getId();
        LinkedHashMap<Long, Coworking> items = new LinkedHashMap<>();
        coworkingRepository.findAllByArchivedTrue()
                .stream()
                .filter(c -> adminId.equals(c.getOwnerId()))
                .forEach(c -> items.put(c.getId(), c));
        accessService.getAccessibleCoworkings(adminId).forEach(access -> coworkingRepository.findById(access.id())
                .filter(Coworking::getArchived)
                .ifPresent(c -> items.putIfAbsent(c.getId(), c)));
        return items.values().stream().map(coworkingMapper::toResponse).toList();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
