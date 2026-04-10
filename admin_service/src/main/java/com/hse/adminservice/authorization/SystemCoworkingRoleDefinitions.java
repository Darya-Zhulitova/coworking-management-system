package com.hse.adminservice.authorization;

import com.hse.adminservice.entity.Grant;
import com.hse.adminservice.entity.Role;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class SystemCoworkingRoleDefinitions {

    public Role buildManagerRole(com.hse.adminservice.entity.Coworking coworking, LocalDateTime now) {
        return Role.builder()
                .coworking(coworking)
                .name("Manager")
                .grantsRaw(serialize(EnumSet.of(
                        Grant.COWORKING_VIEW,
                        Grant.COWORKING_EDIT,
                        Grant.COWORKING_DASHBOARD_VIEW,
                        Grant.PLACE_VIEW,
                        Grant.PLACE_MANAGE,
                        Grant.ACCESS_VIEW,
                        Grant.ACCESS_MANAGE,
                        Grant.ROLE_VIEW,
                        Grant.ROLE_ASSIGN
                )))
                .active(true)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    public Role buildStaffSupportRole(com.hse.adminservice.entity.Coworking coworking, LocalDateTime now) {
        return Role.builder()
                .coworking(coworking)
                .name("Staff support")
                .grantsRaw(serialize(EnumSet.of(
                        Grant.COWORKING_VIEW,
                        Grant.COWORKING_DASHBOARD_VIEW,
                        Grant.PLACE_VIEW
                )))
                .active(true)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    public Set<Grant> parseGrants(String raw) {
        if (raw == null || raw.isBlank()) {
            return EnumSet.noneOf(Grant.class);
        }
        EnumSet<Grant> grants = EnumSet.noneOf(Grant.class);
        Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(Grant::valueOf)
                .forEach(grants::add);
        return grants;
    }

    public String serialize(Set<Grant> grants) {
        return grants.stream()
                .sorted(Comparator.comparing(Enum::name))
                .map(Enum::name)
                .collect(Collectors.joining(","));
    }
}
