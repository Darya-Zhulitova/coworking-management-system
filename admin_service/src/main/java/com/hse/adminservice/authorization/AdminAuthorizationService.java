package com.hse.adminservice.authorization;

import com.hse.adminservice.entity.AdminCoworkingAccess;
import com.hse.adminservice.entity.AdminPrincipalType;
import com.hse.adminservice.exception.ResourceNotFoundException;
import com.hse.adminservice.repository.AdminCoworkingAccessRepository;
import com.hse.adminservice.repository.CoworkingRepository;
import com.hse.adminservice.service.AdminCoworkingAccessService;
import com.hse.adminservice.service.AuthenticatedAdminActorService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.EnumSet;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AdminAuthorizationService {

    private final AuthenticatedAdminActorService authenticatedAdminActorService;
    private final AdminCoworkingAccessRepository adminCoworkingAccessRepository;
    private final CoworkingRepository coworkingRepository;
    private final AdminCoworkingAccessService accessService;

    public ResolvedAdminAccessContext requireGlobalAction(EffectiveAdminAction action) {
        ResolvedAdminAccessContext context = resolveForGlobalScope();
        ensureAllowed(context, action, null);
        return context;
    }

    public ResolvedAdminAccessContext requireCoworkingAction(Long coworkingId, EffectiveAdminAction action) {
        ResolvedAdminAccessContext context = resolveForCoworkingScope(coworkingId);
        ensureAllowed(context, action, coworkingId);
        return context;
    }

    public ResolvedAdminAccessContext resolveForGlobalScope() {
        if (authenticatedAdminActorService.isSuperAdmin()) {
            return ResolvedAdminAccessContext.builder()
                    .principalType(AdminPrincipalType.SUPERADMIN)
                    .superAdminId(authenticatedAdminActorService.getSubjectId())
                    .tenantPermissions(EnumSet.noneOf(TenantPermission.class))
                    .grantedActions(EnumSet.of(
                            EffectiveAdminAction.VIEW_ALL_COWORKINGS,
                            EffectiveAdminAction.VIEW_COWORKING,
                            EffectiveAdminAction.CREATE_COWORKING,
                            EffectiveAdminAction.UPDATE_COWORKING,
                            EffectiveAdminAction.ARCHIVE_COWORKING,
                            EffectiveAdminAction.VIEW_TENANT_DASHBOARD,
                            EffectiveAdminAction.VIEW_ACCESSIBLE_COWORKINGS,
                            EffectiveAdminAction.VIEW_STAFF_ACCESS,
                            EffectiveAdminAction.MANAGE_STAFF_ACCESS,
                            EffectiveAdminAction.VIEW_PLACES,
                            EffectiveAdminAction.MANAGE_PLACES
                    ))
                    .build();
        }

        return ResolvedAdminAccessContext.builder()
                .principalType(AdminPrincipalType.TENANT_ADMIN)
                .tenantAdminUserId(authenticatedAdminActorService.getSubjectId())
                .tenantPermissions(EnumSet.noneOf(TenantPermission.class))
                .grantedActions(EnumSet.of(
                        EffectiveAdminAction.VIEW_ACCESSIBLE_COWORKINGS,
                        EffectiveAdminAction.CREATE_COWORKING
                ))
                .build();
    }

    public ResolvedAdminAccessContext resolveForCoworkingScope(Long coworkingId) {
        if (!coworkingRepository.existsByIdAndArchivedFalse(coworkingId)) {
            throw new ResourceNotFoundException("Coworking not found");
        }

        if (authenticatedAdminActorService.isSuperAdmin()) {
            return ResolvedAdminAccessContext.builder()
                    .principalType(AdminPrincipalType.SUPERADMIN)
                    .superAdminId(authenticatedAdminActorService.getSubjectId())
                    .coworkingId(coworkingId)
                    .tenantPermissions(EnumSet.allOf(TenantPermission.class))
                    .grantedActions(EnumSet.of(
                            EffectiveAdminAction.VIEW_COWORKING,
                            EffectiveAdminAction.UPDATE_COWORKING,
                            EffectiveAdminAction.ARCHIVE_COWORKING,
                            EffectiveAdminAction.VIEW_TENANT_DASHBOARD,
                            EffectiveAdminAction.VIEW_ALL_COWORKINGS,
                            EffectiveAdminAction.VIEW_ACCESSIBLE_COWORKINGS,
                            EffectiveAdminAction.VIEW_STAFF_ACCESS,
                            EffectiveAdminAction.MANAGE_STAFF_ACCESS,
                            EffectiveAdminAction.VIEW_PLACES,
                            EffectiveAdminAction.MANAGE_PLACES
                    ))
                    .build();
        }

        Optional<AdminCoworkingAccess> accessOptional = adminCoworkingAccessRepository
                .findByAdminUserIdAndCoworkingId(authenticatedAdminActorService.getSubjectId(), coworkingId)
                .filter(access -> Boolean.TRUE.equals(access.getActive()) && !Boolean.TRUE.equals(access.getCoworking().getArchived()));

        if (accessOptional.isEmpty()) {
            throw new ResourceNotFoundException("Coworking not found");
        }

        AdminCoworkingAccess access = accessOptional.get();
        return ResolvedAdminAccessContext.builder()
                .principalType(AdminPrincipalType.TENANT_ADMIN)
                .tenantAdminUserId(access.getAdminUser().getId())
                .coworkingId(access.getCoworking().getId())
                .assignmentType(access.getAssignmentType())
                .coworkingRole(access.getRole())
                .tenantPermissions(accessService.resolvePermissions(access))
                .grantedActions(accessService.resolveGrantedActions(access))
                .build();
    }

    private void ensureAllowed(ResolvedAdminAccessContext context, EffectiveAdminAction action, Long coworkingId) {
        if (!context.hasAction(action)) {
            String scope = coworkingId == null ? "global" : "coworking=" + coworkingId;
            throw new AccessDeniedException("Action %s is not allowed for %s scope".formatted(action, scope));
        }
    }
}
