package com.hse.adminservice.auth.service;

import com.hse.adminservice.auth.dto.AuthRequest;
import com.hse.adminservice.auth.dto.AuthResponse;
import com.hse.adminservice.common.security.AdminPrincipalUserDetailsService;
import com.hse.adminservice.common.security.AuthenticatedAdminPrincipal;
import com.hse.adminservice.common.security.JwtService;
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
    private final AdminPrincipalUserDetailsService userDetailsService;

    @Override
    public AuthResponse login(AuthRequest request) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                request.email(),
                request.password()
        ));

        var userDetails = (AuthenticatedAdminPrincipal) userDetailsService.loadUserByUsername(request.email());
        String token = jwtService.generateToken(Map.of("subjectId", userDetails.getSubjectId()), userDetails);

        return new AuthResponse(token, userDetails.getSubjectId());
    }
}
