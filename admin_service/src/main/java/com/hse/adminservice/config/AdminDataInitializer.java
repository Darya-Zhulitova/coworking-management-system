package com.hse.adminservice.config;

import com.hse.adminservice.entity.AdminRole;
import com.hse.adminservice.entity.AdminUser;
import com.hse.adminservice.repository.AdminUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

@Configuration
@RequiredArgsConstructor
public class AdminDataInitializer {

    private final PasswordEncoder passwordEncoder;

    @Bean
    CommandLineRunner initAdmin(AdminUserRepository adminUserRepository) {
        return args -> {
            if (adminUserRepository.findByEmailAndArchivedFalse("admin@test.test").isEmpty()) {
                LocalDateTime now = LocalDateTime.now();

                adminUserRepository.save(AdminUser.builder()
                        .email("admin@test.test")
                        .passwordHash(passwordEncoder.encode("pass"))
                        .role(AdminRole.ADMIN)
                        .active(true)
                        .archived(false)
                        .archivedAt(null)
                        .createdAt(now)
                        .updatedAt(now)
                        .build());
            }
        };
    }
}