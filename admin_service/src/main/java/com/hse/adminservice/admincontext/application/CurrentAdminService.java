package com.hse.adminservice.admincontext.application;

import com.hse.adminservice.adminaccount.domain.Admin;
import com.hse.adminservice.adminaccount.persistence.AdminRepository;
import com.hse.adminservice.common.error.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CurrentAdminService {
    private final AdminRepository adminRepository;
    private final AuthenticatedAdminActorService authenticatedAdminActorService;

    public Admin getCurrentAdmin() {
        return adminRepository.findById(authenticatedAdminActorService.getSubjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Администратор коворкинга не найден"));
    }
}
