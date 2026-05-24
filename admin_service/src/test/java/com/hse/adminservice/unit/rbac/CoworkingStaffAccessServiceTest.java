package com.hse.adminservice.unit.rbac;

import com.hse.adminservice.adminaccount.domain.Admin;
import com.hse.adminservice.adminaccount.persistence.AdminRepository;
import com.hse.adminservice.common.error.ConflictException;
import com.hse.adminservice.common.error.ResourceNotFoundException;
import com.hse.adminservice.common.time.TimeProvider;
import com.hse.adminservice.coworking.domain.Coworking;
import com.hse.adminservice.coworking.persistence.CoworkingRepository;
import com.hse.adminservice.rbac.application.CoworkingStaffAccessService;
import com.hse.adminservice.rbac.authorization.GrantResolver;
import com.hse.adminservice.rbac.authorization.SystemCoworkingRoleDefinitions;
import com.hse.adminservice.rbac.domain.Access;
import com.hse.adminservice.rbac.domain.Grant;
import com.hse.adminservice.rbac.domain.Role;
import com.hse.adminservice.rbac.dto.CoworkingAccessResponse;
import com.hse.adminservice.rbac.mapper.CoworkingAccessMapper;
import com.hse.adminservice.rbac.persistence.AccessRepository;
import com.hse.adminservice.rbac.persistence.RoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CoworkingStaffAccessServiceTest {

    @Mock private AccessRepository accessRepository;
    @Mock private AdminRepository adminRepository;
    @Mock private CoworkingRepository coworkingRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private TimeProvider timeProvider;

    private SystemCoworkingRoleDefinitions definitions;
    private CoworkingStaffAccessService service;

    @BeforeEach
    void setUp() {
        definitions = new SystemCoworkingRoleDefinitions();
        GrantResolver grantResolver = new GrantResolver(definitions);
        service = new CoworkingStaffAccessService(
                accessRepository,
                adminRepository,
                coworkingRepository,
                roleRepository,
                new CoworkingAccessMapper(),
                grantResolver,
                timeProvider
        );
    }

    @Test
    void getCoworkingAccessListMapsAccessesWithResolvedGrants() {
        Access access = access(100L, admin(7L, "staff@example.test"), coworking(1L, 99L), role(55L, "Support", EnumSet.of(Grant.USER_READ), true), true);
        when(accessRepository.findAllByCoworkingIdAndCoworkingArchivedFalse(1L)).thenReturn(List.of(access));

        List<CoworkingAccessResponse> responses = service.getCoworkingAccessList(1L);

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().accessId()).isEqualTo(100L);
        assertThat(responses.getFirst().email()).isEqualTo("staff@example.test");
        assertThat(responses.getFirst().roleName()).isEqualTo("Support");
        assertThat(responses.getFirst().grants()).containsExactly(Grant.USER_READ);
    }

    @Test
    void assignRoleTrimsEmailPersistsActiveAccessAndReturnsHydratedResponse() {
        Admin staff = admin(7L, "staff@example.test");
        Coworking coworking = coworking(1L, 99L);
        Role role = role(55L, "Support", EnumSet.of(Grant.USER_READ), true);
        LocalDateTime now = LocalDateTime.of(2026, 5, 21, 11, 0);
        when(adminRepository.findByEmailIgnoreCase("staff@example.test")).thenReturn(Optional.of(staff));
        when(coworkingRepository.findByIdAndArchivedFalse(1L)).thenReturn(Optional.of(coworking));
        when(roleRepository.findByIdAndCoworkingIdAndActiveTrue(55L, 1L)).thenReturn(Optional.of(role));
        when(accessRepository.findByAdminIdAndCoworkingId(7L, 1L)).thenReturn(Optional.empty());
        when(timeProvider.now()).thenReturn(now);
        when(accessRepository.save(any(Access.class))).thenAnswer(invocation -> {
            Access saved = invocation.getArgument(0);
            saved.setId(100L);
            return saved;
        });
        when(accessRepository.findByIdAndCoworkingIdAndCoworkingArchivedFalse(100L, 1L))
                .thenAnswer(invocation -> Optional.of(access(100L, staff, coworking, role, true)));

        CoworkingAccessResponse response = service.assignRole(1L, " staff@example.test ", 55L);

        assertThat(response.accessId()).isEqualTo(100L);
        assertThat(response.adminId()).isEqualTo(7L);
        assertThat(response.roleId()).isEqualTo(55L);
        assertThat(response.active()).isTrue();
        verify(accessRepository).save(argThat(access ->
                access.getAdmin() == staff
                        && access.getCoworking() == coworking
                        && access.getRole() == role
                        && Boolean.TRUE.equals(access.getActive())
                        && access.getCreatedAt().equals(now)
                        && access.getUpdatedAt().equals(now)
        ));
    }

    @Test
    void assignRoleRejectsOwnerBecauseOwnerIsNotManagedAsStaff() {
        Admin owner = admin(99L, "owner@example.test");
        Coworking coworking = coworking(1L, 99L);
        Role role = role(55L, "Support", EnumSet.of(Grant.USER_READ), true);
        when(adminRepository.findByEmailIgnoreCase("owner@example.test")).thenReturn(Optional.of(owner));
        when(coworkingRepository.findByIdAndArchivedFalse(1L)).thenReturn(Optional.of(coworking));
        when(roleRepository.findByIdAndCoworkingIdAndActiveTrue(55L, 1L)).thenReturn(Optional.of(role));

        assertThatThrownBy(() -> service.assignRole(1L, "owner@example.test", 55L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Владелец не управляется через доступ сотрудников");

        verify(accessRepository, never()).save(any());
    }

    @Test
    void assignRoleRejectsExistingAccessEvenWhenInactive() {
        Admin staff = admin(7L, "staff@example.test");
        Coworking coworking = coworking(1L, 99L);
        Role role = role(55L, "Support", EnumSet.of(Grant.USER_READ), true);
        Access inactiveAccess = access(100L, staff, coworking, role, false);
        when(adminRepository.findByEmailIgnoreCase("staff@example.test")).thenReturn(Optional.of(staff));
        when(coworkingRepository.findByIdAndArchivedFalse(1L)).thenReturn(Optional.of(coworking));
        when(roleRepository.findByIdAndCoworkingIdAndActiveTrue(55L, 1L)).thenReturn(Optional.of(role));
        when(accessRepository.findByAdminIdAndCoworkingId(7L, 1L)).thenReturn(Optional.of(inactiveAccess));

        assertThatThrownBy(() -> service.assignRole(1L, "staff@example.test", 55L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("У администратора уже есть доступ к этому коворкингу");
    }

    @Test
    void assignRoleFailsWhenAdminDoesNotExist() {
        when(adminRepository.findByEmailIgnoreCase("missing@example.test")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.assignRole(1L, "missing@example.test", 55L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Администратор не найден");
    }

    @Test
    void updateAccessRoleChangesRoleActiveFlagAndUpdatedAt() {
        Admin staff = admin(7L, "staff@example.test");
        Coworking coworking = coworking(1L, 99L);
        Role oldRole = role(55L, "Support", EnumSet.of(Grant.USER_READ), true);
        Role newRole = role(56L, "Manager", EnumSet.of(Grant.ACCESS_EDIT), true);
        Access existing = access(100L, staff, coworking, oldRole, true);
        LocalDateTime now = LocalDateTime.of(2026, 5, 21, 12, 0);
        when(accessRepository.findByIdAndCoworkingIdAndCoworkingArchivedFalse(100L, 1L))
                .thenReturn(Optional.of(existing))
                .thenReturn(Optional.of(existing));
        when(roleRepository.findByIdAndCoworkingIdAndActiveTrue(56L, 1L)).thenReturn(Optional.of(newRole));
        when(timeProvider.now()).thenReturn(now);
        when(accessRepository.save(existing)).thenReturn(existing);

        CoworkingAccessResponse response = service.updateAccessRole(1L, 100L, 56L, false);

        assertThat(existing.getRole()).isSameAs(newRole);
        assertThat(existing.getActive()).isFalse();
        assertThat(existing.getUpdatedAt()).isEqualTo(now);
        assertThat(response.roleId()).isEqualTo(56L);
        assertThat(response.active()).isFalse();
        assertThat(response.grants()).isEmpty();
    }

    @Test
    void updateAccessRoleRejectsInactiveOrForeignRole() {
        Access existing = access(100L, admin(7L, "staff@example.test"), coworking(1L, 99L), role(55L, "Support", EnumSet.of(Grant.USER_READ), true), true);
        when(accessRepository.findByIdAndCoworkingIdAndCoworkingArchivedFalse(100L, 1L)).thenReturn(Optional.of(existing));
        when(roleRepository.findByIdAndCoworkingIdAndActiveTrue(56L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateAccessRole(1L, 100L, 56L, true))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Роль не найдена");
    }

    @Test
    void deactivateMarksAccessInactiveAndUpdatesTimestamp() {
        Access existing = access(100L, admin(7L, "staff@example.test"), coworking(1L, 99L), role(55L, "Support", EnumSet.of(Grant.USER_READ), true), true);
        LocalDateTime now = LocalDateTime.of(2026, 5, 21, 12, 30);
        when(accessRepository.findByIdAndCoworkingIdAndCoworkingArchivedFalse(100L, 1L)).thenReturn(Optional.of(existing));
        when(timeProvider.now()).thenReturn(now);

        service.deactivate(1L, 100L);

        assertThat(existing.getActive()).isFalse();
        assertThat(existing.getUpdatedAt()).isEqualTo(now);
        verify(accessRepository).save(existing);
    }

    private static Admin admin(Long id, String email) {
        return Admin.builder().id(id).email(email).name("Admin %s".formatted(id)).build();
    }

    private static Coworking coworking(Long id, Long ownerId) {
        return Coworking.builder().id(id).ownerId(ownerId).archived(false).build();
    }

    private Role role(Long id, String name, EnumSet<Grant> grants, Boolean active) {
        return Role.builder()
                .id(id)
                .name(name)
                .grantsRaw(definitions.serialize(grants))
                .active(active)
                .build();
    }

    private static Access access(Long id, Admin admin, Coworking coworking, Role role, Boolean active) {
        return Access.builder()
                .id(id)
                .admin(admin)
                .coworking(coworking)
                .role(role)
                .active(active)
                .build();
    }
}
