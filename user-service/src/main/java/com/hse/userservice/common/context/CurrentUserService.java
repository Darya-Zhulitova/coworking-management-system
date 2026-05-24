package com.hse.userservice.common.context;

import com.hse.userservice.common.exception.ResourceNotFoundException;
import com.hse.userservice.common.security.AuthenticatedUserPrincipal;
import com.hse.userservice.feature.user.domain.User;
import com.hse.userservice.feature.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CurrentUserService {
    private final UserRepository userRepository;

    public Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            throw new IllegalStateException("Необходимо войти в систему.");
        }
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof AuthenticatedUserPrincipal authenticatedUserPrincipal)) {
            throw new IllegalStateException("Неподдерживаемый тип авторизации.");
        }
        return authenticatedUserPrincipal.getUserId();
    }

    public User getCurrentUser() {
        return userRepository.findById(getCurrentUserId()).orElseThrow(() -> new ResourceNotFoundException(
                "Пользователь не найден."));
    }
}
