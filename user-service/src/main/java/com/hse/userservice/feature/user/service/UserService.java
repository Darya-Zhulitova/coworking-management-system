package com.hse.userservice.feature.user.service;

import com.hse.userservice.common.context.CurrentUserService;
import com.hse.userservice.common.exception.ResourceConflictException;
import com.hse.userservice.common.security.AuthenticatedUserPrincipal;
import com.hse.userservice.common.security.JwtService;
import com.hse.userservice.common.security.UserPrincipalDetailsService;
import com.hse.userservice.feature.user.domain.User;
import com.hse.userservice.feature.user.dto.*;
import com.hse.userservice.feature.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserService {
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserPrincipalDetailsService userDetailsService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CurrentUserService currentUserService;

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                request.email().trim(),
                request.password()
        ));

        AuthenticatedUserPrincipal principal = (AuthenticatedUserPrincipal) userDetailsService.loadUserByUsername(
                request.email().trim());
        String token = jwtService.generateToken(Map.of("subjectId", principal.getUserId()), principal);
        return new AuthResponse(token, principal.getUserId());
    }

    public AuthResponse register(RegisterRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new ResourceConflictException("Пользователь с таким email уже существует.");
        }

        User user = new User();
        user.setEmail(normalizedEmail);
        user.setName(request.name().trim());
        user.setDescription(request.description() == null ? null : request.description().trim());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        userRepository.save(user);

        return login(new LoginRequest(normalizedEmail, request.password()));
    }

    public UserResponse getCurrentUserProfile() {
        User user = currentUserService.getCurrentUser();
        return toProfileDto(user);
    }

    public UserResponse updateCurrentUserProfile(UpdateUserRequest request) {
        User user = currentUserService.getCurrentUser();
        user.setName(request.name().trim());
        user.setDescription(request.description() == null || request.description()
                .isBlank() ? null : request.description().trim());
        return toProfileDto(userRepository.save(user));
    }

    private UserResponse toProfileDto(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getName(), user.getDescription());
    }
}
