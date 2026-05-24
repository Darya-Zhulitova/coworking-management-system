package com.hse.adminservice.unit.rbac;

import com.hse.adminservice.adminaccount.domain.Admin;
import com.hse.adminservice.admincontext.application.AuthenticatedAdminActorService;
import com.hse.adminservice.common.error.ResourceNotFoundException;
import com.hse.adminservice.coworking.domain.Coworking;
import com.hse.adminservice.coworking.persistence.CoworkingRepository;
import com.hse.adminservice.rbac.application.AccessService;
import com.hse.adminservice.rbac.authorization.AccessDeniedException;
import com.hse.adminservice.rbac.authorization.AdminAuthorizationService;
import com.hse.adminservice.rbac.authorization.ResolvedAdminAccessContext;
import com.hse.adminservice.rbac.domain.Access;
import com.hse.adminservice.rbac.domain.Grant;
import com.hse.adminservice.rbac.domain.Role;
import com.hse.adminservice.rbac.persistence.AccessRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.EnumSet;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminAuthorizationServiceTest {

    @Mock private AuthenticatedAdminActorService actorService;
    @Mock private AccessRepository accessRepository;
    @Mock private CoworkingRepository coworkingRepository;
    @Mock private AccessService accessService;

    private AdminAuthorizationService authorizationService;

    @BeforeEach
    void setUp() {
        authorizationService = new AdminAuthorizationService(actorService, accessRepository, coworkingRepository, accessService);
    }

    @Test
    void globalScopeAllowsBuiltInCoworkingActions() {
        when(actorService.getSubjectId()).thenReturn(10L);

        authorizationService.requireGlobalAction(Grant.COWORKING_EDIT);

        verifyNoInteractions(coworkingRepository, accessRepository, accessService);
    }

    @Test
    void globalScopeRejectsNonGlobalAction() {
        when(actorService.getSubjectId()).thenReturn(10L);

        assertThatThrownBy(() -> authorizationService.requireGlobalAction(Grant.TARIFF_EDIT))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Недостаточно прав для выполнения действия");
    }

    @Test
    void coworkingOwnerGetsOwnerGrantSet() {
        Coworking coworking = Coworking.builder().id(1L).ownerId(10L).archived(false).build();
        when(coworkingRepository.findByIdAndArchivedFalse(1L)).thenReturn(Optional.of(coworking));
        when(actorService.getSubjectId()).thenReturn(10L);
        when(accessService.resolveOwnerGrantedActions()).thenReturn(EnumSet.allOf(Grant.class));

        ResolvedAdminAccessContext context = authorizationService.requireCoworkingAction(1L, Grant.PLACE_EDIT);

        assertThat(context.adminId()).isEqualTo(10L);
        assertThat(context.coworkingId()).isEqualTo(1L);
        assertThat(context.owner()).isTrue();
        assertThat(context.grants()).contains(Grant.PLACE_EDIT);
        verifyNoInteractions(accessRepository);
    }

    @Test
    void activeStaffAccessCanPerformGrantedAction() {
        Coworking coworking = Coworking.builder().id(1L).ownerId(99L).archived(false).build();
        Admin staff = Admin.builder().id(10L).build();
        Role role = Role.builder().active(true).build();
        Access access = Access.builder().admin(staff).coworking(coworking).role(role).active(true).build();
        when(coworkingRepository.findByIdAndArchivedFalse(1L)).thenReturn(Optional.of(coworking));
        when(actorService.getSubjectId()).thenReturn(10L);
        when(accessRepository.findByAdminIdAndCoworkingId(10L, 1L)).thenReturn(Optional.of(access));
        when(accessService.resolveGrantedActions(access)).thenReturn(EnumSet.of(Grant.TARIFF_READ));

        ResolvedAdminAccessContext context = authorizationService.requireCoworkingAction(1L, Grant.TARIFF_READ);

        assertThat(context.adminId()).isEqualTo(10L);
        assertThat(context.coworkingId()).isEqualTo(1L);
        assertThat(context.owner()).isFalse();
        assertThat(context.grants()).containsExactly(Grant.TARIFF_READ);
    }

    @Test
    void inactiveStaffAccessIsHiddenAsNotFound() {
        Coworking coworking = Coworking.builder().id(1L).ownerId(99L).archived(false).build();
        Access access = Access.builder()
                .admin(Admin.builder().id(10L).build())
                .coworking(coworking)
                .role(Role.builder().active(true).build())
                .active(false)
                .build();
        when(coworkingRepository.findByIdAndArchivedFalse(1L)).thenReturn(Optional.of(coworking));
        when(actorService.getSubjectId()).thenReturn(10L);
        when(accessRepository.findByAdminIdAndCoworkingId(10L, 1L)).thenReturn(Optional.of(access));

        assertThatThrownBy(() -> authorizationService.requireCoworkingAction(1L, Grant.TARIFF_READ))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Коворкинг не найден");
    }

    @Test
    void staffWithoutGrantReceivesAccessDenied() {
        Coworking coworking = Coworking.builder().id(1L).ownerId(99L).archived(false).build();
        Access access = Access.builder()
                .admin(Admin.builder().id(10L).build())
                .coworking(coworking)
                .role(Role.builder().active(true).build())
                .active(true)
                .build();
        when(coworkingRepository.findByIdAndArchivedFalse(1L)).thenReturn(Optional.of(coworking));
        when(actorService.getSubjectId()).thenReturn(10L);
        when(accessRepository.findByAdminIdAndCoworkingId(10L, 1L)).thenReturn(Optional.of(access));
        when(accessService.resolveGrantedActions(access)).thenReturn(EnumSet.of(Grant.TARIFF_READ));

        assertThatThrownBy(() -> authorizationService.requireCoworkingAction(1L, Grant.TARIFF_EDIT))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Недостаточно прав для выполнения действия");
    }

    @Test
    void missingCoworkingIsNotFoundBeforeAccessResolution() {
        when(coworkingRepository.findByIdAndArchivedFalse(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authorizationService.requireCoworkingAction(404L, Grant.COWORKING_READ))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Коворкинг не найден");

        verifyNoInteractions(accessRepository, accessService);
    }
}
