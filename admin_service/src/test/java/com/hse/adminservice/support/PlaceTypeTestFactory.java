package com.hse.adminservice.support;

import com.hse.adminservice.coworking.domain.Coworking;
import com.hse.adminservice.pricing.tariff.domain.Tariff;
import com.hse.adminservice.space.placetype.domain.PlaceType;

import java.time.LocalDateTime;
import java.util.UUID;

public final class PlaceTypeTestFactory {
    private PlaceTypeTestFactory() {
    }

    public static PlaceType placeType(Coworking coworking, Tariff tariff) {
        LocalDateTime now = LocalDateTime.now();
        return PlaceType.builder()
                .coworking(coworking)
                .tariff(tariff)
                .name("Desk " + UUID.randomUUID())
                .active(true)
                .archived(false)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }
}
