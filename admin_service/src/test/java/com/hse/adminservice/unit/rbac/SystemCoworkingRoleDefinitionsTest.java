package com.hse.adminservice.unit.rbac;

import com.hse.adminservice.coworking.domain.Coworking;
import com.hse.adminservice.rbac.authorization.SystemCoworkingRoleDefinitions;
import com.hse.adminservice.rbac.domain.Grant;
import com.hse.adminservice.rbac.domain.Role;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SystemCoworkingRoleDefinitionsTest {

    private final SystemCoworkingRoleDefinitions definitions = new SystemCoworkingRoleDefinitions();

    @Test
    void serializeSortsGrantsForStablePersistence() {
        String serialized = definitions.serialize(EnumSet.of(Grant.TARIFF_EDIT, Grant.COWORKING_READ, Grant.ACCESS_EDIT));

        assertThat(serialized).isEqualTo("ACCESS_EDIT,COWORKING_READ,TARIFF_EDIT");
    }

    @Test
    void parseGrantsIgnoresBlankSegmentsAndWhitespace() {
        Set<Grant> parsed = definitions.parseGrants(" COWORKING_READ, ,TARIFF_EDIT ");

        assertThat(parsed).containsExactlyInAnyOrder(Grant.COWORKING_READ, Grant.TARIFF_EDIT);
    }

    @Test
    void parseNullAndBlankRawGrantsToEmptySet() {
        assertThat(definitions.parseGrants(null)).isEmpty();
        assertThat(definitions.parseGrants("   ")).isEmpty();
    }

    @Test
    void managerRoleContainsOperationalEditPermissions() {
        Coworking coworking = Coworking.builder().id(1L).build();
        LocalDateTime now = LocalDateTime.of(2026, 5, 21, 10, 0);

        Role role = definitions.buildManagerRole(coworking, now);
        Set<Grant> grants = definitions.parseGrants(role.getGrantsRaw());

        assertThat(role.getCoworking()).isSameAs(coworking);
        assertThat(role.getName()).isEqualTo("Финансовый менеджер");
        assertThat(role.getActive()).isTrue();
        assertThat(role.getCreatedAt()).isEqualTo(now);
        assertThat(role.getUpdatedAt()).isEqualTo(now);
        assertThat(grants).contains(
                Grant.COWORKING_EDIT,
                Grant.FLOOR_EDIT,
                Grant.PLACE_TYPE_EDIT,
                Grant.PLACE_EDIT,
                Grant.TARIFF_EDIT,
                Grant.SERVICE_REQUEST_TYPE_EDIT,
                Grant.ROLE_EDIT,
                Grant.ACCESS_EDIT,
                Grant.SCHEDULE_EDIT
        );
    }

    @Test
    void staffSupportRoleIsReadOnlyForConfigurationAndOperations() {
        Coworking coworking = Coworking.builder().id(1L).build();
        Role role = definitions.buildStaffSupportRole(coworking, LocalDateTime.of(2026, 5, 21, 10, 0));
        Set<Grant> grants = definitions.parseGrants(role.getGrantsRaw());

        assertThat(role.getName()).isEqualTo("Офис-менеджер");
        assertThat(grants).contains(
                Grant.COWORKING_READ,
                Grant.FLOOR_READ,
                Grant.PLACE_TYPE_READ,
                Grant.PLACE_READ,
                Grant.TARIFF_READ,
                Grant.SERVICE_REQUEST_TYPE_READ,
                Grant.ROLE_READ,
                Grant.ACCESS_READ,
                Grant.SCHEDULE_READ,
                Grant.USER_READ,
                Grant.BOOKING_READ
        );
        assertThat(grants).doesNotContain(
                Grant.COWORKING_EDIT,
                Grant.FLOOR_EDIT,
                Grant.PLACE_TYPE_EDIT,
                Grant.PLACE_EDIT,
                Grant.TARIFF_EDIT,
                Grant.SERVICE_REQUEST_TYPE_EDIT,
                Grant.ROLE_EDIT,
                Grant.ACCESS_EDIT,
                Grant.SCHEDULE_EDIT,
                Grant.USER_EDIT,
                Grant.BOOKING_EDIT
        );
    }
}
