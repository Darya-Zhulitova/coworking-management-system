package com.hse.adminservice.service;

import com.hse.adminservice.dto.AuthRequest;
import com.hse.adminservice.dto.AuthResponse;
import com.hse.adminservice.security.AdminPrincipalUserDetailsService;
import com.hse.adminservice.security.AuthenticatedAdminPrincipal;
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
    private final AdminPrincipalUserDetailsService userDetailsService;

    @Override
    public AuthResponse login(AuthRequest request) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                request.getEmail(),
                request.getPassword()
        ));

        var userDetails = (AuthenticatedAdminPrincipal) userDetailsService.loadUserByUsername(request.getEmail());
        String token = jwtService.generateToken(Map.of(
                "subjectId", userDetails.getSubjectId(),
                "principalType", userDetails.getPrincipalType().name()
        ), userDetails);

        return AuthResponse.builder()
                .token(token)
                .adminId(userDetails.getSubjectId())
                                                                .build();
    }

}
