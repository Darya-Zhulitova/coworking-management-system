package com.hse.adminservice.service;

import com.hse.adminservice.authorization.EffectiveAdminAction;
import com.hse.adminservice.dto.AuthRequest;
import com.hse.adminservice.dto.AuthResponse;
import com.hse.adminservice.entity.AdminPrincipalType;
import com.hse.adminservice.security.AdminPrincipalUserDetailsService;
import com.hse.adminservice.security.AuthenticatedAdminPrincipal;
import com.hse.adminservice.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final AdminPrincipalUserDetailsService userDetailsService;
    private final AdminCoworkingAccessService accessService;

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

        var accessibleCoworkings = userDetails.getPrincipalType() == AdminPrincipalType.SUPERADMIN
                ? java.util.List.<com.hse.adminservice.dto.AccessibleCoworkingResponse>of()
                : accessService.getAccessibleCoworkings(userDetails.getSubjectId());

        return AuthResponse.builder()
                .token(token)
                .adminUserId(userDetails.getPrincipalType() == AdminPrincipalType.TENANT_ADMIN ? userDetails.getSubjectId() : null)
                .superAdminId(userDetails.getPrincipalType() == AdminPrincipalType.SUPERADMIN ? userDetails.getSubjectId() : null)
                .principalType(userDetails.getPrincipalType())
                .grantedGlobalActions(resolveGlobalActions(userDetails.getPrincipalType()))
                .coworkings(accessibleCoworkings)
                .build();
    }

    private Set<EffectiveAdminAction> resolveGlobalActions(AdminPrincipalType principalType) {
        if (principalType == AdminPrincipalType.SUPERADMIN) {
            return EnumSet.of(
                    EffectiveAdminAction.VIEW_ALL_COWORKINGS,
                    EffectiveAdminAction.VIEW_COWORKING,
                    EffectiveAdminAction.CREATE_COWORKING,
                    EffectiveAdminAction.UPDATE_COWORKING,
                    EffectiveAdminAction.ARCHIVE_COWORKING,
                    EffectiveAdminAction.VIEW_TENANT_DASHBOARD,
                    EffectiveAdminAction.VIEW_ACCESSIBLE_COWORKINGS
            );
        }

        return EnumSet.of(
                EffectiveAdminAction.VIEW_ACCESSIBLE_COWORKINGS,
                EffectiveAdminAction.CREATE_COWORKING
        );
    }
}
