package com.hse.adminservice.config;

import com.hse.adminservice.entity.AdminCoworkingRole;
import com.hse.adminservice.entity.AdminUser;
import com.hse.adminservice.entity.Coworking;
import com.hse.adminservice.entity.SuperAdmin;
import com.hse.adminservice.repository.AdminUserRepository;
import com.hse.adminservice.repository.CoworkingRepository;
import com.hse.adminservice.repository.SuperAdminRepository;
import com.hse.adminservice.service.AdminCoworkingAccessService;
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
    private static final String DEFAULT_OWNER_PASSWORD = "pass";
    private static final String DEFAULT_COWORKING_NAME = "Default Coworking";
    private static final String DEFAULT_SUPERADMIN_EMAIL = "superadmin@test.test";
    private static final String DEFAULT_SUPERADMIN_PASSWORD = "pass";
    private static final String SECOND_OWNER_EMAIL = "manager@test.test";
    private static final String SECOND_OWNER_PASSWORD = "pass";
    private static final String SECOND_COWORKING_NAME = "Second Coworking";
    private static final String STAFF_MANAGER_EMAIL = "staff1@test.test";
    private static final String STAFF_MANAGER_PASSWORD = "pass";
    private static final String STAFF_SUPPORT_EMAIL = "support1@test.test";
    private static final String STAFF_SUPPORT_PASSWORD = "pass";

    private final PasswordEncoder passwordEncoder;
    private final AdminCoworkingAccessService accessService;

    @Bean
    CommandLineRunner initAdmin(
            AdminUserRepository adminUserRepository,
            SuperAdminRepository superAdminRepository,
            CoworkingRepository coworkingRepository
    ) {
        return args -> {
            LocalDateTime now = LocalDateTime.now();

            AdminUser adminUser = adminUserRepository.findByEmailAndArchivedFalse(DEFAULT_OWNER_EMAIL)
                    .orElseGet(() -> adminUserRepository.save(AdminUser.builder()
                            .email(DEFAULT_OWNER_EMAIL)
                            .passwordHash(passwordEncoder.encode(DEFAULT_OWNER_PASSWORD))
                            .active(true)
                            .archived(false)
                            .archivedAt(null)
                            .createdAt(now)
                            .updatedAt(now)
                            .build()));

            superAdminRepository.findByEmailAndArchivedFalse(DEFAULT_SUPERADMIN_EMAIL)
                    .orElseGet(() -> superAdminRepository.save(SuperAdmin.builder()
                            .email(DEFAULT_SUPERADMIN_EMAIL)
                            .passwordHash(passwordEncoder.encode(DEFAULT_SUPERADMIN_PASSWORD))
                            .active(true)
                            .archived(false)
                            .archivedAt(null)
                            .createdAt(now)
                            .updatedAt(now)
                            .build()));

            AdminUser secondAdminUser = adminUserRepository.findByEmailAndArchivedFalse(SECOND_OWNER_EMAIL)
                    .orElseGet(() -> adminUserRepository.save(AdminUser.builder()
                            .email(SECOND_OWNER_EMAIL)
                            .passwordHash(passwordEncoder.encode(SECOND_OWNER_PASSWORD))
                            .active(true)
                            .archived(false)
                            .archivedAt(null)
                            .createdAt(now)
                            .updatedAt(now)
                            .build()));

            AdminUser staffManager = adminUserRepository.findByEmailAndArchivedFalse(STAFF_MANAGER_EMAIL)
                    .orElseGet(() -> adminUserRepository.save(AdminUser.builder()
                            .email(STAFF_MANAGER_EMAIL)
                            .passwordHash(passwordEncoder.encode(STAFF_MANAGER_PASSWORD))
                            .active(true)
                            .archived(false)
                            .archivedAt(null)
                            .createdAt(now)
                            .updatedAt(now)
                            .build()));

            AdminUser staffSupport = adminUserRepository.findByEmailAndArchivedFalse(STAFF_SUPPORT_EMAIL)
                    .orElseGet(() -> adminUserRepository.save(AdminUser.builder()
                            .email(STAFF_SUPPORT_EMAIL)
                            .passwordHash(passwordEncoder.encode(STAFF_SUPPORT_PASSWORD))
                            .active(true)
                            .archived(false)
                            .archivedAt(null)
                            .createdAt(now)
                            .updatedAt(now)
                            .build()));

            Coworking coworking = coworkingRepository.findAllByArchivedFalse()
                    .stream()
                    .filter(existing -> DEFAULT_COWORKING_NAME.equals(existing.getName()))
                    .findFirst()
                    .orElseGet(() -> coworkingRepository.save(Coworking.builder()
                            .name(DEFAULT_COWORKING_NAME)
                            .active(true)
                            .archived(false)
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
                            .active(true)
                            .archived(false)
                            .archivedAt(null)
                            .createdAt(now)
                            .updatedAt(now)
                            .build()));

            accessService.grantOwnerAccess(adminUser, coworking);
            accessService.grantOwnerAccess(secondAdminUser, secondCoworking);
            try {
                accessService.assignRole(coworking.getId(), STAFF_MANAGER_EMAIL, AdminCoworkingRole.MANAGER);
            } catch (Exception ignored) {}
            try {
                accessService.assignRole(coworking.getId(), STAFF_SUPPORT_EMAIL, AdminCoworkingRole.STAFF_SUPPORT);
            } catch (Exception ignored) {}
        };
    }
}
