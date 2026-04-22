package com.hse.adminservice.context.service;

import com.hse.adminservice.common.exception.ResourceNotFoundException;
import com.hse.adminservice.rbac.entity.Admin;
import com.hse.adminservice.rbac.repository.AdminRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CurrentAdminService {
    private final AdminRepository adminRepository;
    private final AuthenticatedAdminActorService authenticatedAdminActorService;

    public Admin getCurrentAdmin() {
        return adminRepository.findById(authenticatedAdminActorService.getSubjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated coworking admin not found"));
    }
}
