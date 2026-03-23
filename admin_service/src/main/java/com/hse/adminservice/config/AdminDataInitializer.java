package com.hse.adminservice.config;

import com.hse.adminservice.entity.AdminUser;
import com.hse.adminservice.entity.Coworking;
import com.hse.adminservice.repository.AdminUserRepository;
import com.hse.adminservice.repository.CoworkingRepository;
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

    private final PasswordEncoder passwordEncoder;
    private final AdminCoworkingAccessService accessService;

    @Bean
    CommandLineRunner initAdmin(AdminUserRepository adminUserRepository, CoworkingRepository coworkingRepository) {
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

            Coworking coworking = coworkingRepository.findAllByArchivedFalse()
                    .stream()
                    .findFirst()
                    .orElseGet(() -> coworkingRepository.save(Coworking.builder()
                            .name(DEFAULT_COWORKING_NAME)
                            .active(true)
                            .archived(false)
                            .archivedAt(null)
                            .createdAt(now)
                            .updatedAt(now)
                            .build()));

            accessService.grantOwnerAccess(adminUser, coworking);
        };
    }
}
