package com.hse.adminservice.unit.rbac;

import com.hse.adminservice.common.error.ConflictException;
import com.hse.adminservice.common.error.ResourceNotFoundException;
import com.hse.adminservice.common.time.TimeProvider;
import com.hse.adminservice.coworking.domain.Coworking;
import com.hse.adminservice.coworking.persistence.CoworkingRepository;
import com.hse.adminservice.rbac.application.CoworkingRoleService;
import com.hse.adminservice.rbac.authorization.GrantResolver;
import com.hse.adminservice.rbac.authorization.SystemCoworkingRoleDefinitions;
import com.hse.adminservice.rbac.domain.Grant;
import com.hse.adminservice.rbac.domain.Role;
import com.hse.adminservice.rbac.dto.CoworkingRoleResponse;
import com.hse.adminservice.rbac.mapper.CoworkingAccessMapper;
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
class CoworkingRoleServiceTest {

    @Mock private CoworkingRepository coworkingRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private TimeProvider timeProvider;

    private SystemCoworkingRoleDefinitions definitions;
    private GrantResolver grantResolver;
    private CoworkingRoleService service;

    @BeforeEach
    void setUp() {
        definitions = new SystemCoworkingRoleDefinitions();
        grantResolver = new GrantResolver(definitions);
        service = new CoworkingRoleService(
                coworkingRepository,
                roleRepository,
                definitions,
                new CoworkingAccessMapper(),
                grantResolver,
                timeProvider
        );
    }

    @Test
    void getAllRolesMapsStoredRoleGrants() {
        Role readOnly = role(1L, "Reader", EnumSet.of(Grant.TARIFF_READ), true);
        Role editor = role(2L, "Editor", EnumSet.of(Grant.TARIFF_EDIT), true);
        when(roleRepository.findAllByCoworkingIdOrderByNameAsc(10L)).thenReturn(List.of(readOnly, editor));

        List<CoworkingRoleResponse> result = service.getAllRoles(10L);

        assertThat(result).extracting(CoworkingRoleResponse::name).containsExactly("Reader", "Editor");
        assertThat(result.get(1).grants()).containsExactlyInAnyOrder(Grant.TARIFF_EDIT, Grant.TARIFF_READ);
    }

    @Test
    void createRoleTrimsNameNormalizesGrantsAndDefaultsActiveToTrue() {
        Coworking coworking = Coworking.builder().id(10L).build();
        LocalDateTime now = LocalDateTime.of(2026, 5, 21, 12, 0);
        when(coworkingRepository.findByIdAndArchivedFalse(10L)).thenReturn(Optional.of(coworking));
        when(roleRepository.existsByCoworkingIdAndNameIgnoreCase(10L, "Operators")).thenReturn(false);
        when(timeProvider.now()).thenReturn(now);
        when(roleRepository.save(any(Role.class))).thenAnswer(invocation -> {
            Role role = invocation.getArgument(0);
            role.setId(100L);
            return role;
        });

        CoworkingRoleResponse response = service.createRole(10L, "  Operators  ", EnumSet.of(Grant.PLACE_EDIT), null);

        assertThat(response.roleId()).isEqualTo(100L);
        assertThat(response.name()).isEqualTo("Operators");
        assertThat(response.active()).isTrue();
        assertThat(response.grants()).containsExactlyInAnyOrder(Grant.PLACE_EDIT, Grant.PLACE_READ);
        verify(roleRepository).save(argThat(role ->
                role.getCoworking() == coworking
                        && role.getName().equals("Operators")
                        && role.getActive().equals(Boolean.TRUE)
                        && role.getCreatedAt().equals(now)
                        && role.getUpdatedAt().equals(now)
                        && definitions.parseGrants(role.getGrantsRaw()).containsAll(EnumSet.of(Grant.PLACE_EDIT, Grant.PLACE_READ))
        ));
    }

    @Test
    void createRoleCanPersistInactiveRole() {
        when(coworkingRepository.findByIdAndArchivedFalse(10L)).thenReturn(Optional.of(Coworking.builder().id(10L).build()));
        when(roleRepository.existsByCoworkingIdAndNameIgnoreCase(10L, "Disabled")).thenReturn(false);
        when(timeProvider.now()).thenReturn(LocalDateTime.of(2026, 5, 21, 12, 0));
        when(roleRepository.save(any(Role.class))).thenAnswer(invocation -> {
            Role role = invocation.getArgument(0);
            role.setId(101L);
            return role;
        });

        CoworkingRoleResponse response = service.createRole(10L, "Disabled", EnumSet.of(Grant.ROLE_READ), false);

        assertThat(response.active()).isFalse();
        verify(roleRepository).save(argThat(role -> Boolean.FALSE.equals(role.getActive())));
    }

    @Test
    void createRoleRejectsDuplicateNameWithinCoworking() {
        when(coworkingRepository.findByIdAndArchivedFalse(10L)).thenReturn(Optional.of(Coworking.builder().id(10L).build()));
        when(roleRepository.existsByCoworkingIdAndNameIgnoreCase(10L, "Operators")).thenReturn(true);

        assertThatThrownBy(() -> service.createRole(10L, " Operators ", EnumSet.of(Grant.PLACE_READ), true))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Роль с таким названием уже существует в этом коворкинге");

        verify(roleRepository, never()).save(any());
    }

    @Test
    void updateRoleDefinitionRejectsDuplicateNameOutsideCurrentRole() {
        Role existing = role(3L, "Old", EnumSet.of(Grant.PLACE_READ), true);
        when(roleRepository.findByIdAndCoworkingId(3L, 10L)).thenReturn(Optional.of(existing));
        when(roleRepository.existsByCoworkingIdAndNameIgnoreCaseAndIdNot(10L, "New", 3L)).thenReturn(true);

        assertThatThrownBy(() -> service.updateRoleDefinition(10L, 3L, " New ", EnumSet.of(Grant.ACCESS_READ), true))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Роль с таким названием уже существует в этом коворкинге");
    }

    @Test
    void updateRoleDefinitionPersistsNormalizedGrantsAndTimestamp() {
        Role existing = role(3L, "Old", EnumSet.of(Grant.PLACE_READ), true);
        LocalDateTime now = LocalDateTime.of(2026, 5, 22, 9, 30);
        when(roleRepository.findByIdAndCoworkingId(3L, 10L)).thenReturn(Optional.of(existing));
        when(roleRepository.existsByCoworkingIdAndNameIgnoreCaseAndIdNot(10L, "Updated", 3L)).thenReturn(false);
        when(timeProvider.now()).thenReturn(now);
        when(roleRepository.save(existing)).thenReturn(existing);

        CoworkingRoleResponse response = service.updateRoleDefinition(10L, 3L, " Updated ", EnumSet.of(Grant.ACCESS_EDIT), false);

        assertThat(response.name()).isEqualTo("Updated");
        assertThat(response.active()).isFalse();
        assertThat(response.grants()).isEmpty();
        assertThat(definitions.parseGrants(existing.getGrantsRaw()))
                .containsExactlyInAnyOrder(Grant.ACCESS_EDIT, Grant.ACCESS_READ);
        assertThat(existing.getUpdatedAt()).isEqualTo(now);
    }

    @Test
    void archiveRoleMarksRoleInactive() {
        Role existing = role(3L, "Old", EnumSet.of(Grant.PLACE_READ), true);
        LocalDateTime now = LocalDateTime.of(2026, 5, 22, 9, 30);
        when(roleRepository.findByIdAndCoworkingId(3L, 10L)).thenReturn(Optional.of(existing));
        when(timeProvider.now()).thenReturn(now);

        service.archiveRole(10L, 3L);

        assertThat(existing.getActive()).isFalse();
        assertThat(existing.getUpdatedAt()).isEqualTo(now);
        verify(roleRepository).save(existing);
    }

    @Test
    void missingRoleThrowsNotFound() {
        when(roleRepository.findByIdAndCoworkingId(3L, 10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getRole(10L, 3L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Роль не найдена");
    }

    private Role role(Long id, String name, EnumSet<Grant> grants, Boolean active) {
        return Role.builder()
                .id(id)
                .coworking(Coworking.builder().id(10L).build())
                .name(name)
                .grantsRaw(definitions.serialize(grants))
                .active(active)
                .createdAt(LocalDateTime.of(2026, 5, 21, 10, 0))
                .updatedAt(LocalDateTime.of(2026, 5, 21, 10, 0))
                .build();
    }
}
