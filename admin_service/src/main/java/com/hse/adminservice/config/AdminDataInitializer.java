package com.hse.adminservice.config;

import com.hse.adminservice.authorization.SystemCoworkingRoleDefinitions;
import com.hse.adminservice.entity.Admin;
import com.hse.adminservice.entity.Coworking;
import com.hse.adminservice.entity.CoworkingPlaceType;
import com.hse.adminservice.entity.Place;
import com.hse.adminservice.entity.Role;
import com.hse.adminservice.repository.*;
import com.hse.adminservice.service.AccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

@Configuration
@RequiredArgsConstructor
public class AdminDataInitializer {

    private static final String DEFAULT_OWNER_EMAIL = "admin@test.test";
    private static final String DEFAULT_OWNER_NAME = "Default Owner";
    private static final String DEFAULT_OWNER_PASSWORD = "pass";
    private static final String DEFAULT_COWORKING_NAME = "Default Coworking";
    private static final String SECOND_OWNER_EMAIL = "manager@test.test";
    private static final String SECOND_OWNER_NAME = "Second Owner";
    private static final String SECOND_OWNER_PASSWORD = "pass";
    private static final String SECOND_COWORKING_NAME = "Second Coworking";
    private static final String STAFF_MANAGER_EMAIL = "staff1@test.test";
    private static final String STAFF_MANAGER_NAME = "Staff Manager";
    private static final String STAFF_MANAGER_PASSWORD = "pass";
    private static final String STAFF_SUPPORT_EMAIL = "support1@test.test";
    private static final String STAFF_SUPPORT_NAME = "Support Agent";
    private static final String STAFF_SUPPORT_PASSWORD = "pass";

    private final PasswordEncoder passwordEncoder;
    private final AccessService accessService;
    private final SystemCoworkingRoleDefinitions systemCoworkingRoleDefinitions;

    @Bean
    CommandLineRunner initAdmin(
            AdminRepository adminRepository,
            CoworkingRepository coworkingRepository,
            RoleRepository roleRepository,
            CoworkingPlaceTypeRepository placeTypeRepository,
            PlaceRepository placeRepository
    ) {
        return args -> {
            LocalDateTime now = LocalDateTime.now();

            Admin admin = adminRepository.findByEmailIgnoreCase(DEFAULT_OWNER_EMAIL)
                    .orElseGet(() -> adminRepository.save(Admin.builder()
                            .email(DEFAULT_OWNER_EMAIL)
                            .name(DEFAULT_OWNER_NAME)
                            .passwordHash(passwordEncoder.encode(DEFAULT_OWNER_PASSWORD))
                            .createdAt(now)
                            .updatedAt(now)
                            .build()));

            Admin secondAdmin = adminRepository.findByEmailIgnoreCase(SECOND_OWNER_EMAIL)
                    .orElseGet(() -> adminRepository.save(Admin.builder()
                            .email(SECOND_OWNER_EMAIL)
                            .name(SECOND_OWNER_NAME)
                            .passwordHash(passwordEncoder.encode(SECOND_OWNER_PASSWORD))
                            .createdAt(now)
                            .updatedAt(now)
                            .build()));

            adminRepository.findByEmailIgnoreCase(STAFF_MANAGER_EMAIL)
                    .orElseGet(() -> adminRepository.save(Admin.builder()
                            .email(STAFF_MANAGER_EMAIL)
                            .name(STAFF_MANAGER_NAME)
                            .passwordHash(passwordEncoder.encode(STAFF_MANAGER_PASSWORD))
                            .createdAt(now)
                            .updatedAt(now)
                            .build()));

            adminRepository.findByEmailIgnoreCase(STAFF_SUPPORT_EMAIL)
                    .orElseGet(() -> adminRepository.save(Admin.builder()
                            .email(STAFF_SUPPORT_EMAIL)
                            .name(STAFF_SUPPORT_NAME)
                            .passwordHash(passwordEncoder.encode(STAFF_SUPPORT_PASSWORD))
                            .createdAt(now)
                            .updatedAt(now)
                            .build()));

            Coworking coworking = coworkingRepository.findAllByArchivedFalse()
                    .stream()
                    .filter(existing -> DEFAULT_COWORKING_NAME.equals(existing.getName()))
                    .findFirst()
                    .orElseGet(() -> coworkingRepository.save(Coworking.builder()
                            .name(DEFAULT_COWORKING_NAME)
                            .schedule(127)
                            .ownerId(admin.getId())
                            .active(true)
                            .archived(false)
                            .configurationVersion(0L)
                            .archivedAt(null)
                            .createdAt(now)
                            .updatedAt(now)
                            .build()));

            Coworking secondCoworking = coworkingRepository.findAllByArchivedFalse()
                    .stream()
                    .filter(existing -> SECOND_COWORKING_NAME.equals(existing.getName()))
                    .findFirst()
                    .orElseGet(() -> coworkingRepository.save(Coworking.builder()
                            .name(SECOND_COWORKING_NAME)
                            .schedule(127)
                            .ownerId(secondAdmin.getId())
                            .active(true)
                            .archived(false)
                            .configurationVersion(0L)
                            .archivedAt(null)
                            .createdAt(now)
                            .updatedAt(now)
                            .build()));

            ensureSystemRoles(roleRepository, coworking, now);
            ensureSystemRoles(roleRepository, secondCoworking, now);

            Role managerRole = roleRepository.findAllByCoworkingIdAndActiveTrueOrderByNameAsc(coworking.getId()).stream()
                    .filter(role -> "Manager".equals(role.getName()))
                    .findFirst()
                    .orElseThrow();
            Role staffSupportRole = roleRepository.findAllByCoworkingIdAndActiveTrueOrderByNameAsc(coworking.getId()).stream()
                    .filter(role -> "Staff support".equals(role.getName()))
                    .findFirst()
                    .orElseThrow();

            try {
                accessService.assignRole(coworking.getId(), STAFF_MANAGER_EMAIL, managerRole.getId());
            } catch (Exception ignored) {}
            try {
                accessService.assignRole(coworking.getId(), STAFF_SUPPORT_EMAIL, staffSupportRole.getId());
            } catch (Exception ignored) {}

            seedPlaceType(placeTypeRepository, coworking, "DESK", "Desk", "Open-space workstation", now);
            seedPlaceType(placeTypeRepository, coworking, "MEETING_ROOM", "Meeting room", "Meeting room for small teams", now);
            seedPlaceType(placeTypeRepository, secondCoworking, "OFFICE", "Private office", "Dedicated enclosed office", now);
            seedPlaceType(placeTypeRepository, secondCoworking, "ROOM", "Focus room", "Quiet room for calls and focus work", now);

            CoworkingPlaceType desk = placeTypeRepository.findAllByCoworkingIdAndArchivedFalseOrderByNameAsc(coworking.getId()).stream()
                    .filter(type -> "DESK".equals(type.getCode()))
                    .findFirst()
                    .orElseThrow();
            CoworkingPlaceType meeting = placeTypeRepository.findAllByCoworkingIdAndArchivedFalseOrderByNameAsc(coworking.getId()).stream()
                    .filter(type -> "MEETING_ROOM".equals(type.getCode()))
                    .findFirst()
                    .orElseThrow();
            CoworkingPlaceType office = placeTypeRepository.findAllByCoworkingIdAndArchivedFalseOrderByNameAsc(secondCoworking.getId()).stream()
                    .filter(type -> "OFFICE".equals(type.getCode()))
                    .findFirst()
                    .orElseThrow();
            CoworkingPlaceType room = placeTypeRepository.findAllByCoworkingIdAndArchivedFalseOrderByNameAsc(secondCoworking.getId()).stream()
                    .filter(type -> "ROOM".equals(type.getCode()))
                    .findFirst()
                    .orElseThrow();

            seedPlace(placeRepository, coworking, desk, "D-01", true, false, now);
            seedPlace(placeRepository, coworking, desk, "D-02", false, false, now);
            seedPlace(placeRepository, coworking, meeting, "M-01", true, false, now);
            seedPlace(placeRepository, secondCoworking, office, "O-01", true, false, now);
            seedPlace(placeRepository, secondCoworking, room, "R-01", true, false, now);
            seedPlace(placeRepository, secondCoworking, room, "R-ARCH", false, true, now);

            coworkingRepository.save(coworking);
            coworkingRepository.save(secondCoworking);
        };
    }

    private void ensureSystemRoles(RoleRepository repository, Coworking coworking, LocalDateTime now) {
        if (!repository.existsByCoworkingIdAndNameIgnoreCase(coworking.getId(), "Manager")) {
            repository.save(systemCoworkingRoleDefinitions.buildManagerRole(coworking, now));
        }
        if (!repository.existsByCoworkingIdAndNameIgnoreCase(coworking.getId(), "Staff support")) {
            repository.save(systemCoworkingRoleDefinitions.buildStaffSupportRole(coworking, now));
        }
    }

    private void seedPlaceType(CoworkingPlaceTypeRepository repository, Coworking coworking, String code, String name, String description, LocalDateTime now) {
        boolean exists = repository.findAllByCoworkingIdAndArchivedFalseOrderByNameAsc(coworking.getId()).stream()
                .anyMatch(type -> code.equals(type.getCode()));
        if (exists) {
            return;
        }
        repository.save(CoworkingPlaceType.builder()
                .coworking(coworking)
                .code(code)
                .name(name)
                .description(description)
                .active(true)
                .archived(false)
                .archivedAt(null)
                .createdAt(now)
                .updatedAt(now)
                .build());
        coworking.setConfigurationVersion((coworking.getConfigurationVersion() == null ? 0L : coworking.getConfigurationVersion()) + 1L);
    }

    private void seedPlace(PlaceRepository repository, Coworking coworking, CoworkingPlaceType placeType, String name, boolean active, boolean archived, LocalDateTime now) {
        boolean exists = repository.findAllByCoworkingIdAndArchivedFalseOrderByNameAsc(coworking.getId()).stream()
                .anyMatch(place -> name.equals(place.getName()));
        if (exists && !archived) {
            return;
        }
        if (archived) {
            boolean archivedExists = repository.findAll().stream().anyMatch(place -> place.getCoworking().getId().equals(coworking.getId()) && name.equals(place.getName()));
            if (archivedExists) return;
        }
        repository.save(Place.builder()
                .coworking(coworking)
                .placeType(placeType)
                .name(name)
                .active(active)
                .archived(archived)
                .archivedAt(archived ? now : null)
                .createdAt(now)
                .updatedAt(now)
                .build());
        coworking.setConfigurationVersion((coworking.getConfigurationVersion() == null ? 0L : coworking.getConfigurationVersion()) + 1L);
    }
}
