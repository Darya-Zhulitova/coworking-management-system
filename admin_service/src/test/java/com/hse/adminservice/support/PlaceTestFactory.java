package com.hse.adminservice.support;

import com.hse.adminservice.coworking.domain.Coworking;
import com.hse.adminservice.space.floor.domain.Floor;
import com.hse.adminservice.space.place.domain.Place;
import com.hse.adminservice.space.placetype.domain.PlaceType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public final class PlaceTestFactory {
    private PlaceTestFactory() {
    }

    public static Place place(Coworking coworking, Floor floor, PlaceType placeType) {
        LocalDateTime now = LocalDateTime.now();
        return Place.builder()
                .coworking(coworking)
                .floor(floor)
                .placeType(placeType)
                .name("A-" + UUID.randomUUID())
                .locX(new BigDecimal("10.0000"))
                .locY(new BigDecimal("20.0000"))
                .imageFileId("place-image-test")
                .amenitiesRaw("monitor,coffee")
                .active(true)
                .archived(false)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }
}
