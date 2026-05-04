package com.hse.adminservice.space.place.validation;

import com.hse.adminservice.common.error.ConflictException;
import com.hse.adminservice.space.place.domain.Place;
import org.springframework.stereotype.Component;

@Component
public class PlaceActivationValidator {
    public void validateCanActivate(Place place) {
        if (!Boolean.TRUE.equals(place.getPlaceType().getActive())) {
            throw new ConflictException("Cannot activate place while its place type is inactive");
        }
        if (!Boolean.TRUE.equals(place.getFloor().getActive())) {
            throw new ConflictException("Cannot activate place while its floor is inactive");
        }
    }
}
