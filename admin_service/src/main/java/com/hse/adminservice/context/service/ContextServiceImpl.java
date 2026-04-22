package com.hse.adminservice.context.service;

import com.hse.adminservice.common.exception.ResourceNotFoundException;
import com.hse.adminservice.context.dto.AppContextResponse;
import com.hse.adminservice.coworking.entity.Coworking;
import com.hse.adminservice.coworking.repository.CoworkingRepository;
import com.hse.adminservice.rbac.authorization.AdminAuthorizationService;
import com.hse.adminservice.rbac.service.AccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContextServiceImpl implements ContextService {
    private final CurrentAdminService currentAdminService;
    private final AdminAuthorizationService authorizationService;
    private final CoworkingRepository coworkingRepository;
    private final AccessService accessService;

    @Override
    public AppContextResponse getContext(Long coworkingId) {
        var admin = currentAdminService.getCurrentAdmin();
        if (coworkingId == null) {
            var context = authorizationService.resolveForGlobalScope();
            return AppContextResponse.builder().id(admin.getId()).email(admin.getEmail()).name(admin.getName()).role(
                    null).grants(context.grants()).coworkingId(null).coworkingName(null).build();
        }

        var context = authorizationService.resolveForCoworkingScope(coworkingId);
        Coworking coworking = coworkingRepository.findByIdAndArchivedFalse(coworkingId)
                .orElseThrow(() -> new ResourceNotFoundException("Coworking not found"));

        return AppContextResponse.builder().id(admin.getId()).email(admin.getEmail()).name(admin.getName()).role(
                accessService.resolveDisplayAccessLabel(admin.getId(), coworking)).grants(context.grants()).coworkingId(
                coworking.getId()).coworkingName(coworking.getName()).build();
    }
}
