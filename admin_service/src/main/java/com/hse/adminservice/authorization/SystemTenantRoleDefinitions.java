package com.hse.adminservice.authorization;

import com.hse.adminservice.entity.AdminCoworkingRole;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@Component
public class SystemTenantRoleDefinitions {

    private final Map<AdminCoworkingRole, Set<TenantPermission>> permissionsByRole;

    public SystemTenantRoleDefinitions() {
        Map<AdminCoworkingRole, Set<TenantPermission>> definitions = new EnumMap<>(AdminCoworkingRole.class);
        definitions.put(AdminCoworkingRole.MANAGER, EnumSet.of(
                TenantPermission.VIEW_COWORKING_DETAILS,
                TenantPermission.EDIT_COWORKING_DETAILS,
                TenantPermission.VIEW_TENANT_DASHBOARD,
                TenantPermission.VIEW_PLACES,
                TenantPermission.MANAGE_PLACES,
                TenantPermission.VIEW_STAFF_ACCESS
        ));
        definitions.put(AdminCoworkingRole.STAFF_SUPPORT, EnumSet.of(
                TenantPermission.VIEW_COWORKING_DETAILS,
                TenantPermission.VIEW_TENANT_DASHBOARD,
                TenantPermission.VIEW_PLACES
        ));
        this.permissionsByRole = Map.copyOf(definitions);
    }

    public Set<TenantPermission> getPermissions(AdminCoworkingRole role) {
        return permissionsByRole.getOrDefault(role, EnumSet.noneOf(TenantPermission.class));
    }
}
