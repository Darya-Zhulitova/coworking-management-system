package com.hse.adminservice.authorization;

import com.hse.adminservice.entity.Access;
import com.hse.adminservice.entity.AdminPrincipalType;
import com.hse.adminservice.entity.Grant;
import com.hse.adminservice.entity.Coworking;
import com.hse.adminservice.exception.ResourceNotFoundException;
import com.hse.adminservice.repository.AccessRepository;
import com.hse.adminservice.repository.CoworkingRepository;
import com.hse.adminservice.service.AccessService;
import com.hse.adminservice.service.AuthenticatedAdminActorService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.EnumSet;

@Service
@RequiredArgsConstructor
public class AdminAuthorizationService {

    private final AuthenticatedAdminActorService authenticatedAdminActorService;
    private final AccessRepository adminCoworkingAccessRepository;
    private final CoworkingRepository coworkingRepository;
    private final AccessService accessService;

    public ResolvedAdminAccessContext requireGlobalAction(Grant action) {
        ResolvedAdminAccessContext context = resolveForGlobalScope();
        ensureAllowed(context, action, null);
        return context;
    }

    public ResolvedAdminAccessContext requireCoworkingAction(Long coworkingId, Grant action) {
        ResolvedAdminAccessContext context = resolveForCoworkingScope(coworkingId);
        ensureAllowed(context, action, coworkingId);
        return context;
    }

    public ResolvedAdminAccessContext resolveForGlobalScope() {
        return ResolvedAdminAccessContext.builder()
                .principalType(AdminPrincipalType.COWORKING_ADMIN)
                .coworkingAdminId(authenticatedAdminActorService.getSubjectId())
                .grants(EnumSet.of(
                        Grant.COWORKING_LIST,
                        Grant.COWORKING_CREATE
                ))
                .build();
    }

    public ResolvedAdminAccessContext resolveForCoworkingScope(Long coworkingId) {
        Coworking coworking = coworkingRepository.findByIdAndArchivedFalse(coworkingId)
                .orElseThrow(() -> new ResourceNotFoundException("Coworking not found"));

        Long adminId = authenticatedAdminActorService.getSubjectId();
        if (adminId.equals(coworking.getOwnerId())) {
            return ResolvedAdminAccessContext.builder()
                    .principalType(AdminPrincipalType.COWORKING_ADMIN)
                    .coworkingAdminId(adminId)
                    .coworkingId(coworkingId)
                    .owner(true)
                    .grants(accessService.resolveOwnerGrantedActions())
                    .build();
        }

        Access access = adminCoworkingAccessRepository
                .findByAdminIdAndCoworkingId(adminId, coworkingId)
                .filter(found -> Boolean.TRUE.equals(found.getActive()) && !Boolean.TRUE.equals(found.getCoworking().getArchived()))
                .orElseThrow(() -> new ResourceNotFoundException("Coworking not found"));

        return ResolvedAdminAccessContext.builder()
                .principalType(AdminPrincipalType.COWORKING_ADMIN)
                .coworkingAdminId(access.getAdmin().getId())
                .coworkingId(access.getCoworking().getId())
                .owner(false)
                .grants(accessService.resolveGrantedActions(access))
                .build();
    }

    private void ensureAllowed(ResolvedAdminAccessContext context, Grant action, Long coworkingId) {
        if (!context.hasAction(action)) {
            String scope = coworkingId == null ? "global" : "coworking=" + coworkingId;
            throw new AccessDeniedException("Action %s is not allowed for %s scope".formatted(action, scope));
        }
    }
}
