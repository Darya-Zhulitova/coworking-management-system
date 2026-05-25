package com.hse.adminservice.admincontext.application;

import com.hse.adminservice.common.error.ResourceNotFoundException;
import com.hse.adminservice.common.security.AuthenticatedAdminPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class AuthenticatedAdminActorService {
    public AuthenticatedAdminPrincipal getCurrentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedAdminPrincipal principal)) {
            throw new ResourceNotFoundException("Администратор не найден");
        }
        return principal;
    }

    public Long getSubjectId() {
        return getCurrentPrincipal().getSubjectId();
    }
}
