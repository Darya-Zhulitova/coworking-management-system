package com.hse.adminservice.support;

import com.hse.adminservice.adminaccount.domain.Admin;

import java.time.LocalDateTime;
import java.util.UUID;

public final class AdminTestFactory {
    private AdminTestFactory() {
    }

    public static Admin admin(String passwordHash) {
        LocalDateTime now = LocalDateTime.now();
        return Admin.builder()
                .email("admin-" + UUID.randomUUID() + "@example.test")
                .name("Test Admin")
                .passwordHash(passwordHash)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }
}
