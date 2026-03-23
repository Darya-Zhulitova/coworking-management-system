package com.hse.adminservice.service;

import com.hse.adminservice.entity.AdminUser;
import com.hse.adminservice.exception.ResourceNotFoundException;
import com.hse.adminservice.repository.AdminUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CurrentAdminService {

    private final AdminUserRepository adminUserRepository;

    public AdminUser getCurrentAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new ResourceNotFoundException("Authenticated admin not found");
        }

        return adminUserRepository.findByEmailAndArchivedFalse(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated admin not found"));
    }
}
