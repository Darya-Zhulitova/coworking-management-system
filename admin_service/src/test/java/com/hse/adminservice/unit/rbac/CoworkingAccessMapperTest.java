package com.hse.adminservice.unit.rbac;

import com.hse.adminservice.adminaccount.domain.Admin;
import com.hse.adminservice.rbac.domain.Access;
import com.hse.adminservice.rbac.domain.Grant;
import com.hse.adminservice.rbac.domain.Role;
import com.hse.adminservice.rbac.dto.CoworkingAccessResponse;
import com.hse.adminservice.rbac.dto.CoworkingRoleResponse;
import com.hse.adminservice.rbac.mapper.CoworkingAccessMapper;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.assertj.core.api.Assertions.assertThat;

class CoworkingAccessMapperTest {

    private final CoworkingAccessMapper mapper = new CoworkingAccessMapper();

    @Test
    void mapsStaffAccessToResponse() {
        Admin admin = Admin.builder().id(11L).email("staff@example.test").name("Staff Admin").build();
        Role role = Role.builder().id(22L).name("Operator").build();
        Access access = Access.builder().id(33L).admin(admin).role(role).active(true).build();

        CoworkingAccessResponse response = mapper.toResponse(access, EnumSet.of(Grant.PLACE_READ));

        assertThat(response.accessId()).isEqualTo(33L);
        assertThat(response.adminId()).isEqualTo(11L);
        assertThat(response.email()).isEqualTo("staff@example.test");
        assertThat(response.name()).isEqualTo("Staff Admin");
        assertThat(response.roleId()).isEqualTo(22L);
        assertThat(response.roleName()).isEqualTo("Operator");
        assertThat(response.active()).isTrue();
        assertThat(response.grants()).containsExactly(Grant.PLACE_READ);
    }

    @Test
    void nullAccessActiveFlagIsMappedToFalse() {
        Admin admin = Admin.builder().id(11L).email("staff@example.test").name("Staff Admin").build();
        Role role = Role.builder().id(22L).name("Operator").build();
        Access access = Access.builder().id(33L).admin(admin).role(role).active(null).build();

        CoworkingAccessResponse response = mapper.toResponse(access, EnumSet.of(Grant.PLACE_READ));

        assertThat(response.active()).isFalse();
    }

    @Test
    void mapsRoleToResponse() {
        Role role = Role.builder().id(44L).name("Manager").active(true).build();

        CoworkingRoleResponse response = mapper.toRoleResponse(role, EnumSet.of(Grant.ACCESS_EDIT, Grant.ACCESS_READ));

        assertThat(response.roleId()).isEqualTo(44L);
        assertThat(response.name()).isEqualTo("Manager");
        assertThat(response.active()).isTrue();
        assertThat(response.grants()).containsExactlyInAnyOrder(Grant.ACCESS_EDIT, Grant.ACCESS_READ);
    }

    @Test
    void nullRoleActiveFlagIsMappedToFalse() {
        Role role = Role.builder().id(44L).name("Manager").active(null).build();

        CoworkingRoleResponse response = mapper.toRoleResponse(role, EnumSet.of(Grant.ACCESS_READ));

        assertThat(response.active()).isFalse();
    }
}
