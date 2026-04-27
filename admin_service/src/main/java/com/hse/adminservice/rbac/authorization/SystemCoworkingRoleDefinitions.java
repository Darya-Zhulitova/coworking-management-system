package com.hse.adminservice.rbac.authorization;

import com.hse.adminservice.coworking.domain.Coworking;
import com.hse.adminservice.rbac.domain.Grant;
import com.hse.adminservice.rbac.domain.Role;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class SystemCoworkingRoleDefinitions {
    public Role buildManagerRole(Coworking coworking, LocalDateTime now) {
        return Role.builder().coworking(coworking).name("Финансовый менеджер").grantsRaw(serialize(EnumSet.of(
                Grant.COWORKING_READ,
                Grant.COWORKING_EDIT,
                Grant.FLOOR_READ,
                Grant.FLOOR_EDIT,
                Grant.PLACE_TYPE_READ,
                Grant.PLACE_TYPE_EDIT,
                Grant.PLACE_READ,
                Grant.PLACE_EDIT,
                Grant.TARIFF_READ,
                Grant.TARIFF_EDIT,
                Grant.SERVICE_REQUEST_TYPE_READ,
                Grant.SERVICE_REQUEST_TYPE_EDIT,
                Grant.ROLE_READ,
                Grant.ROLE_EDIT,
                Grant.ACCESS_READ,
                Grant.ACCESS_EDIT,
                Grant.SCHEDULE_READ,
                Grant.SCHEDULE_EDIT,
                Grant.USER_READ,
                Grant.BOOKING_READ
        ))).active(true).createdAt(now).updatedAt(now).build();
    }

    public Role buildStaffSupportRole(Coworking coworking, LocalDateTime now) {
        return Role.builder().coworking(coworking).name("Офис-менеджер").grantsRaw(serialize(EnumSet.of(
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
        ))).active(true).createdAt(now).updatedAt(now).build();
    }

    public String serialize(Set<Grant> grants) {
        return grants.stream().map(Enum::name).sorted().collect(Collectors.joining(","));
    }

    public Set<Grant> parseGrants(String grantsRaw) {
        if (grantsRaw == null || grantsRaw.isBlank()) {
            return EnumSet.noneOf(Grant.class);
        }
        return Arrays.stream(grantsRaw.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(Grant::valueOf)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(Grant.class)));
    }
}
