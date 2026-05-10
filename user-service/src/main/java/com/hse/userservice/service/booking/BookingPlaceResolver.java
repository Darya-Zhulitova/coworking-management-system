package com.hse.userservice.service.booking;

import com.hse.userservice.client.dto.CoworkingConfigSnapshot;
import com.hse.userservice.exception.ResourceConflictException;
import com.hse.userservice.exception.ResourceNotFoundException;
import org.springframework.stereotype.Component;

@Component
public class BookingPlaceResolver {
    public ResolvedPlace resolve(Long placeId, BookingSnapshotContext context, Long coworkingId) {
        CoworkingConfigSnapshot.Place place = context.placesById().get(placeId);
        if (place == null) {
            throw new ResourceNotFoundException("Place not found: " + placeId);
        }
        CoworkingConfigSnapshot.PlaceType placeType = context.placeTypesById().get(place.placeTypeId());
        if (placeType == null) {
            throw new ResourceConflictException("Place type not found for place " + placeId);
        }
        CoworkingConfigSnapshot.Tariff tariff = context.tariffsById().get(placeType.tariffId());
        if (tariff == null) {
            throw new ResourceConflictException("Tariff not found for place " + placeId);
        }
        CoworkingConfigSnapshot.Floor floor = context.floorsById().get(place.floorId());
        if (floor == null) {
            throw new ResourceConflictException("Floor not found for place " + placeId);
        }
        if (!Boolean.TRUE.equals(place.active()) || !Boolean.TRUE.equals(placeType.active()) || !Boolean.TRUE.equals(
                tariff.active())) {
            throw new ResourceConflictException("Place " + placeId + " is not available for booking.");
        }
        return new ResolvedPlace(place, placeType, tariff, floor, coworkingId);
    }
}
