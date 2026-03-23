package com.hse.adminservice.service;

import com.hse.adminservice.dto.AuthRequest;
import com.hse.adminservice.dto.AuthResponse;
import com.hse.adminservice.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final com.hse.adminservice.security.AdminUserDetailsService userDetailsService;
    private final com.hse.adminservice.repository.AdminUserRepository adminUserRepository;
    private final AdminCoworkingAccessService accessService;

    @Override
    public AuthResponse login(AuthRequest request) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                request.getEmail(),
                request.getPassword()
        ));

        var userDetails = userDetailsService.loadUserByUsername(request.getEmail());
        var adminUser = adminUserRepository.findByEmailAndArchivedFalse(request.getEmail())
                .orElseThrow(() -> new IllegalStateException("Authenticated admin user not found"));

        String token = jwtService.generateToken(Map.of("adminUserId", adminUser.getId()), userDetails);

        return AuthResponse.builder()
                .token(token)
                .adminUserId(adminUser.getId())
                .coworkings(accessService.getAccessibleCoworkings(adminUser.getId()))
                .build();
    }
}
