package com.hse.adminservice.support;

import com.hse.adminservice.adminaccount.domain.Admin;
import com.hse.adminservice.coworking.domain.Coworking;
import com.hse.adminservice.rbac.domain.Access;
import com.hse.adminservice.rbac.domain.Grant;
import com.hse.adminservice.rbac.domain.Role;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public final class RbacTestFactory {
    private RbacTestFactory() {
    }

    public static Role role(Coworking coworking, Set<Grant> grants) {
        LocalDateTime now = LocalDateTime.now();
        return Role.builder()
                .coworking(coworking)
                .name("Test role " + now.getNano())
                .grantsRaw(serialize(grants))
                .active(true)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    public static Access access(Admin admin, Coworking coworking, Role role) {
        LocalDateTime now = LocalDateTime.now();
        return Access.builder()
                .admin(admin)
                .coworking(coworking)
                .role(role)
                .active(true)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    private static String serialize(Set<Grant> grants) {
        return grants.stream().map(Enum::name).sorted().collect(Collectors.joining(","));
    }

    public static Set<Grant> grants(Grant... grants) {
        return Arrays.stream(grants).collect(Collectors.toSet());
    }
}
