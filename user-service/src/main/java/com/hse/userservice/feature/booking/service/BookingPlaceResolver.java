package com.hse.userservice.feature.booking.service;

import com.hse.userservice.common.exception.ResourceConflictException;
import com.hse.userservice.common.exception.ResourceNotFoundException;
import com.hse.userservice.integration.dto.BookingContext;
import org.springframework.stereotype.Component;

@Component
public class BookingPlaceResolver {
    public ResolvedPlace resolve(Long placeId, BookingSnapshotContext context, Long coworkingId) {
        BookingContext.Place place = context.placesById().get(placeId);
        if (place == null) {
            throw new ResourceNotFoundException("Место не найдено: " + placeId);
        }
        BookingContext.PlaceType placeType = context.placeTypesById().get(place.placeTypeId());
        if (placeType == null) {
            throw new ResourceConflictException("Тип места не найден для места " + placeId);
        }
        BookingContext.Tariff tariff = context.tariffsById().get(placeType.tariffId());
        if (tariff == null) {
            throw new ResourceConflictException("Тариф не найден для места " + placeId);
        }
        BookingContext.Floor floor = context.floorsById().get(place.floorId());
        if (floor == null) {
            throw new ResourceConflictException("Этаж не найден для места " + placeId);
        }
        if (!Boolean.TRUE.equals(place.active()) || !Boolean.TRUE.equals(placeType.active()) || !Boolean.TRUE.equals(
                tariff.active())) {
            throw new ResourceConflictException("Место " + placeId + " недоступно для бронирования.");
        }
        return new ResolvedPlace(place, placeType, tariff, floor, coworkingId);
    }
}
