package com.hse.adminservice.support;

import com.hse.adminservice.coworking.domain.Coworking;
import com.hse.adminservice.space.floor.domain.Floor;

import java.time.LocalDateTime;

public final class FloorTestFactory {
    private FloorTestFactory() {
    }

    public static Floor floor(Coworking coworking) {
        LocalDateTime now = LocalDateTime.now();
        return Floor.builder()
                .coworking(coworking)
                .name("First floor")
                .index(1)
                .imageFileId("floor-map-test")
                .active(true)
                .archived(false)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }
}
