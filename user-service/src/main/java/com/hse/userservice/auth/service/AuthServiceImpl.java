package com.hse.userservice.auth.service;

import com.hse.userservice.auth.dto.AuthResponse;
import com.hse.userservice.auth.dto.LoginRequest;
import com.hse.userservice.auth.dto.RegisterRequest;
import com.hse.userservice.common.security.AuthenticatedUserPrincipal;
import com.hse.userservice.common.security.JwtService;
import com.hse.userservice.common.security.UserPrincipalDetailsService;
import com.hse.userservice.context.service.CurrentUserService;
import com.hse.userservice.domain.user.User;
import com.hse.userservice.dto.response.UserProfileDto;
import com.hse.userservice.exception.ResourceConflictException;
import com.hse.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserPrincipalDetailsService userDetailsService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CurrentUserService currentUserService;

    @Override
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

    @Override
    public AuthResponse register(RegisterRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new ResourceConflictException("User with this email already exists.");
        }

        User user = new User();
        user.setEmail(normalizedEmail);
        user.setName(request.name().trim());
        user.setDescription(request.description() == null ? null : request.description().trim());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        userRepository.save(user);

        return login(new LoginRequest(normalizedEmail, request.password()));
    }

    @Override
    public UserProfileDto getCurrentUserProfile() {
        User user = currentUserService.getCurrentUser();
        return new UserProfileDto(user.getId(), user.getEmail(), user.getName(), user.getDescription());
    }
}
