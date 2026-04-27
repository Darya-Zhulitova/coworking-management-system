package com.hse.adminservice.space.place.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record PlaceCreateRequest(
        @NotBlank String name,
        @NotNull Long floorId,
        @NotNull Long placeTypeId,
        BigDecimal locX,
        BigDecimal locY,
        List<String> amenities
) {
}
