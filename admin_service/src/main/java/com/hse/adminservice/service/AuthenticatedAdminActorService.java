package com.hse.adminservice.service;

import com.hse.adminservice.entity.AdminPrincipalType;
import com.hse.adminservice.exception.ResourceNotFoundException;
import com.hse.adminservice.security.AuthenticatedAdminPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class AuthenticatedAdminActorService {

    public AuthenticatedAdminPrincipal getCurrentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedAdminPrincipal principal)) {
            throw new ResourceNotFoundException("Authenticated admin subject not found");
        }
        return principal;
    }

    public AdminPrincipalType getPrincipalType() {
        return getCurrentPrincipal().getPrincipalType();
    }

    public Long getSubjectId() {
        return getCurrentPrincipal().getSubjectId();
    }

    public boolean isSuperAdmin() {
        return getPrincipalType() == AdminPrincipalType.SUPERADMIN;
    }
}
