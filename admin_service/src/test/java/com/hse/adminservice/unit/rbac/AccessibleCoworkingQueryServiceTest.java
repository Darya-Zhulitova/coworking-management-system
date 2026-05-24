package com.hse.adminservice.unit.rbac;

import com.hse.adminservice.adminaccount.domain.Admin;
import com.hse.adminservice.common.error.ResourceNotFoundException;
import com.hse.adminservice.coworking.domain.Coworking;
import com.hse.adminservice.coworking.dto.CoworkingListItemResponse;
import com.hse.adminservice.coworking.persistence.CoworkingRepository;
import com.hse.adminservice.rbac.application.AccessibleCoworkingQueryService;
import com.hse.adminservice.rbac.domain.Access;
import com.hse.adminservice.rbac.domain.Role;
import com.hse.adminservice.rbac.persistence.AccessRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccessibleCoworkingQueryServiceTest {

    @Mock private AccessRepository accessRepository;
    @Mock private CoworkingRepository coworkingRepository;

    private AccessibleCoworkingQueryService service;

    @BeforeEach
    void setUp() {
        service = new AccessibleCoworkingQueryService(accessRepository, coworkingRepository);
    }

    @Test
    void returnsOwnedCoworkingsAndActiveStaffAccessWithoutDuplicates() {
        Long adminId = 7L;
        Coworking owned = coworking(1L, "Owned", adminId, true, false);
        Coworking staffOnly = coworking(2L, "Staff", 99L, true, false);
        Access duplicateOwnerAccess = access(adminId, owned, "Manager", true);
        Access staffAccess = access(adminId, staffOnly, "Support", true);
        when(coworkingRepository.findAllByOwnerIdAndArchivedFalse(adminId)).thenReturn(List.of(owned));
        when(accessRepository.findAllByAdminIdAndActiveTrueAndCoworkingArchivedFalse(adminId))
                .thenReturn(List.of(duplicateOwnerAccess, staffAccess));

        List<CoworkingListItemResponse> result = service.getAccessibleCoworkings(adminId);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).id()).isEqualTo(1L);
        assertThat(result.get(0).role()).isEqualTo("Owner");
        assertThat(result.get(1).id()).isEqualTo(2L);
        assertThat(result.get(1).role()).isEqualTo("Support");
    }

    @Test
    void ownerDisplayLabelIsOwnerWithoutRepositoryLookup() {
        Coworking coworking = coworking(1L, "Owned", 7L, true, false);

        assertThat(service.resolveDisplayAccessLabel(7L, coworking)).isEqualTo("Owner");
    }

    @Test
    void staffDisplayLabelUsesActiveAccessRoleName() {
        Coworking coworking = coworking(1L, "Staff", 99L, true, false);
        Access access = access(7L, coworking, "Operator", true);
        when(accessRepository.findByAdminIdAndCoworkingId(7L, 1L)).thenReturn(Optional.of(access));

        assertThat(service.resolveDisplayAccessLabel(7L, coworking)).isEqualTo("Operator");
    }

    @Test
    void inactiveOrMissingStaffAccessIsHiddenAsCoworkingNotFound() {
        Coworking coworking = coworking(1L, "Staff", 99L, true, false);
        Access inactiveAccess = access(7L, coworking, "Operator", false);
        when(accessRepository.findByAdminIdAndCoworkingId(7L, 1L)).thenReturn(Optional.of(inactiveAccess));

        assertThatThrownBy(() -> service.resolveDisplayAccessLabel(7L, coworking))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Коворкинг не найден");
    }

    @Test
    void archivedCoworkingAccessIsHiddenAsCoworkingNotFound() {
        Coworking archivedCoworking = coworking(1L, "Archived", 99L, false, true);
        Access access = access(7L, archivedCoworking, "Operator", true);
        when(accessRepository.findByAdminIdAndCoworkingId(7L, 1L)).thenReturn(Optional.of(access));

        assertThatThrownBy(() -> service.resolveDisplayAccessLabel(7L, archivedCoworking))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Коворкинг не найден");
    }

    private static Coworking coworking(Long id, String name, Long ownerId, Boolean active, Boolean archived) {
        return Coworking.builder()
                .id(id)
                .name(name)
                .ownerId(ownerId)
                .active(active)
                .archived(archived)
                .build();
    }

    private static Access access(Long adminId, Coworking coworking, String roleName, Boolean active) {
        return Access.builder()
                .admin(Admin.builder().id(adminId).build())
                .coworking(coworking)
                .role(Role.builder().name(roleName).active(true).build())
                .active(active)
                .build();
    }
}
