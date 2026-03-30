package com.hse.adminservice.service;

import com.hse.adminservice.entity.AdminPrincipalType;
import com.hse.adminservice.entity.AdminUser;
import com.hse.adminservice.exception.ResourceNotFoundException;
import com.hse.adminservice.repository.AdminUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CurrentAdminService {

    private final AdminUserRepository adminUserRepository;
    private final AuthenticatedAdminActorService authenticatedAdminActorService;

    public AdminUser getCurrentAdmin() {
        if (authenticatedAdminActorService.getPrincipalType() != AdminPrincipalType.TENANT_ADMIN) {
            throw new ResourceNotFoundException("Authenticated tenant admin not found");
        }

        return adminUserRepository.findById(authenticatedAdminActorService.getSubjectId())
                .filter(adminUser -> !Boolean.TRUE.equals(adminUser.getArchived()))
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated tenant admin not found"));
    }
}
