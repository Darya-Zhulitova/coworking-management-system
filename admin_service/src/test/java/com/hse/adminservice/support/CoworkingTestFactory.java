package com.hse.adminservice.support;

import com.hse.adminservice.coworking.domain.Coworking;

import java.time.LocalDateTime;
import java.util.UUID;

public final class CoworkingTestFactory {
    private CoworkingTestFactory() {
    }

    public static Coworking coworking(Long ownerId) {
        LocalDateTime now = LocalDateTime.now();
        return Coworking.builder()
                .name("Volga Hub " + UUID.randomUUID())
                .description("Test coworking")
                .address("Nizhny Novgorod")
                .workingHoursLabel("09:00-21:00")
                .heroTitle("Work here")
                .heroText("Reliable test coworking")
                .imageUrlsJson("[]")
                .imageFileIdsJson("[]")
                .schedule(127)
                .ownerId(ownerId)
                .autoApproveMembership(false)
                .floorMapEnabled(true)
                .joinToken(UUID.randomUUID().toString())
                .active(true)
                .archived(false)
                .configurationVersion(1L)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }
}
